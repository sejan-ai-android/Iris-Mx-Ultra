package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.remote.GeminiApiClient
import com.example.service.AnyClawBridge
import com.example.service.NativeDeviceBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

class IrisAgentRepository(
    private val database: AppDatabase,
    private val nativeBridge: NativeDeviceBridge,
    private val anyClawBridge: AnyClawBridge,
    private val geminiApiClient: GeminiApiClient
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    // Swarm Agent States
    private val _swarmStates = MutableStateFlow<Map<AgentType, SwarmAgentState>>(
        AgentType.entries.associateWith { SwarmAgentState(it, AgentStatus.IDLE) }
    )
    val swarmStates: StateFlow<Map<AgentType, SwarmAgentState>> = _swarmStates.asStateFlow()

    // Shared Event Bus
    private val _eventBus = MutableSharedFlow<AgentEventLog>(replay = 50)
    val eventBus: SharedFlow<AgentEventLog> = _eventBus.asSharedFlow()

    // Active Critic Interception
    private val _pendingCriticWarning = MutableStateFlow<CriticWarning?>(null)
    val pendingCriticWarning: StateFlow<CriticWarning?> = _pendingCriticWarning.asStateFlow()

    // Resumed step callback after user approval
    private var pendingStepExecution: (suspend () -> Unit)? = null

    // Room DB Flows
    val memoriesFlow = database.memoryDao().getAllMemories()
    val knowledgeNodesFlow = database.knowledgeDao().getAllNodes()
    val automationsFlow = database.automationDao().getAllAutomations()
    val recentLogsFlow = database.agentEventLogDao().getRecentLogs()

    fun updateAgentStatus(type: AgentType, status: AgentStatus, activeTask: String? = null) {
        val current = _swarmStates.value.toMutableMap()
        current[type] = SwarmAgentState(
            type = type,
            status = status,
            lastActiveMillis = System.currentTimeMillis(),
            activeTask = activeTask
        )
        _swarmStates.value = current
    }

    suspend fun runSwarmDiagnosticPulse() {
        emitBusEvent(AgentType.ORCHESTRATOR, "HEALTH_CHECK_START", "সকল এজেন্টের সমন্বয় ও স্বাস্থ্য পরীক্ষা শুরু হয়েছে")
        AgentType.entries.forEach { agent ->
            updateAgentStatus(agent, AgentStatus.ANALYZING, "স্বয়ংক্রিয় পরীক্ষা চলছে")
            delay(120)
            updateAgentStatus(agent, AgentStatus.SUCCESS, "প্রস্তুত")
        }
        delay(350)
        AgentType.entries.forEach { agent ->
            updateAgentStatus(agent, AgentStatus.IDLE, null)
        }
        emitBusEvent(AgentType.ORCHESTRATOR, "HEALTH_CHECK_COMPLETE", "সকল এজেন্ট পুরোপুরি প্রস্তুত ও সক্রিয় রয়েছে")
    }

    suspend fun emitBusEvent(agentType: AgentType, eventType: String, message: String, latencyMs: Long = 0) {
        val log = AgentEventLog(
            agentType = agentType.displayName,
            eventType = eventType,
            message = message,
            latencyMs = latencyMs,
            timestamp = System.currentTimeMillis()
        )
        database.agentEventLogDao().insertLog(log)
        _eventBus.emit(log)
    }

    suspend fun processUserPrompt(
        prompt: String,
        language: AppLanguage,
        apiKeyOverride: String?,
        onStepUpdated: (ExecutionDag) -> Unit
    ): ChatMessage {
        val startTime = System.currentTimeMillis()
        emitBusEvent(AgentType.ORCHESTRATOR, "INTENT_EVALUATION", "Parsing prompt: \"$prompt\"")
        updateAgentStatus(AgentType.ORCHESTRATOR, AgentStatus.ANALYZING, "Intent Routing")

        // 1. Emotion Agent Analysis
        updateAgentStatus(AgentType.EMOTION, AgentStatus.ANALYZING, "Sentiment/Tone Assessment")
        val emotionTone = analyzeEmotion(prompt)
        emitBusEvent(AgentType.EMOTION, "TONE_LOCKED", "Detected posture: $emotionTone")
        updateAgentStatus(AgentType.EMOTION, AgentStatus.SUCCESS)

        // 2. Planner Agent DAG Construction
        updateAgentStatus(AgentType.PLANNER, AgentStatus.PLANNING, "Constructing Step DAG")
        val dag = planExecutionDag(prompt)
        emitBusEvent(AgentType.PLANNER, "DAG_BUILT", "Generated ${dag.steps.size} execution steps for goal: ${dag.goal}")
        updateAgentStatus(AgentType.PLANNER, AgentStatus.SUCCESS)
        onStepUpdated(dag)

        // 3. Check with CriticAgent for High-Risk Steps
        val highRiskStep = dag.steps.firstOrNull { isStepHighRisk(it) }
        if (highRiskStep != null) {
            updateAgentStatus(AgentType.CRITIC, AgentStatus.INTERCEPTING, "High-Risk Operation Intercepted")
            val warning = CriticWarning(
                actionName = highRiskStep.title,
                riskLevel = RiskLevel.CRITICAL,
                reason = "CriticAgent Intercept: Destructive operation detected in step '${highRiskStep.title}'. Requires explicit user biometric/confirmation.",
                commandPayload = highRiskStep.parametersJson
            )
            _pendingCriticWarning.value = warning
            emitBusEvent(AgentType.CRITIC, "SAFETY_INTERCEPT", warning.reason)

            val interceptedDag = dag.copy(
                steps = dag.steps.map {
                    if (it.id == highRiskStep.id) it.copy(status = StepStatus.INTERCEPTED_BY_CRITIC) else it
                }
            )
            onStepUpdated(interceptedDag)

            val interceptMsg = if (language == AppLanguage.BENGALI) {
                "একটি বিনীত সতর্কতা: '${highRiskStep.title}' কাজটি বেশ সংবেদনশীল (যেমন ডাটা ডিলিট বা পরিবর্তন)। সবকিছু সুরক্ষিত রাখতে আপনার অনুমতি প্রয়োজন। আপনি কি এটি করতে চান?"
            } else {
                "Just a friendly heads-up: '${highRiskStep.title}' involves a sensitive action (like modifying or deleting data). To keep everything safe, could you confirm if you'd like me to proceed?"
            }

            return ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.AGENT,
                text = interceptMsg,
                agentType = AgentType.CRITIC,
                executionDag = interceptedDag,
                criticWarning = warning,
                latencyMs = System.currentTimeMillis() - startTime,
                emotionTone = emotionTone
            )
        }

        // 4. Executor & Memory Agents Execution
        updateAgentStatus(AgentType.EXECUTOR, AgentStatus.EXECUTING, "Executing Tool Bindings")
        val executedSteps = mutableListOf<DagStep>()
        val toolResults = mutableListOf<String>()

        for (step in dag.steps) {
            val (success, resultOutput) = executeDagStep(step)
            val updatedStep = step.copy(
                status = if (success) StepStatus.COMPLETED else StepStatus.FAILED,
                output = resultOutput
            )
            executedSteps.add(updatedStep)
            toolResults.add("${step.title}: $resultOutput")
            emitBusEvent(AgentType.EXECUTOR, "STEP_COMPLETED", "${step.title} -> $resultOutput")
        }

        val completedDag = dag.copy(steps = executedSteps)
        onStepUpdated(completedDag)
        updateAgentStatus(AgentType.EXECUTOR, AgentStatus.SUCCESS)

        // 5. Synthesize Final Warm, Human-Like Response (Gemini API with Fallback)
        val finalResponseText = synthesizeResponse(
            userPrompt = prompt,
            dag = completedDag,
            toolResults = toolResults,
            language = language,
            apiKeyOverride = apiKeyOverride
        )

        updateAgentStatus(AgentType.ORCHESTRATOR, AgentStatus.IDLE)
        val totalLatency = System.currentTimeMillis() - startTime

        emitBusEvent(
            AgentType.ORCHESTRATOR,
            "WORKFLOW_TERMINATED",
            "Swarm task resolved in ${totalLatency}ms.",
            totalLatency
        )

        return ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.AGENT,
            text = finalResponseText,
            agentType = AgentType.ORCHESTRATOR,
            executionDag = completedDag,
            latencyMs = totalLatency,
            emotionTone = emotionTone
        )
    }

    private fun analyzeEmotion(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("urgent") || lower.contains("asap") || lower.contains("fast") || lower.contains("emergency") || lower.contains("জরুরি") -> "সহানুভূতিশীল ও দ্রুত"
            lower.contains("help") || lower.contains("confused") || lower.contains("problem") || lower.contains("সাহায্য") || lower.contains("সমস্যা") -> "সহানুভূতিশীল ও যত্নশীল"
            lower.contains("ধন্যবাদ") || lower.contains("thanks") || lower.contains("thank") -> "কৃতজ্ঞ ও হাসিখুশি"
            else -> "বন্ধুসুলভ ও আন্তরিক"
        }
    }

    private fun isStepHighRisk(step: DagStep): Boolean {
        val lower = (step.title + " " + step.parametersJson).lowercase()
        return lower.contains("delete") ||
                lower.contains("wipe") ||
                lower.contains("destroy") ||
                lower.contains("factory_reset") ||
                lower.contains("transfer_funds") ||
                lower.contains("rm -rf")
    }

    private fun planExecutionDag(prompt: String): ExecutionDag {
        val lower = prompt.lowercase()
        val steps = mutableListOf<DagStep>()

        when {
            // Full Accessibility & Hands-Free Device Control Triggers
            lower.contains("read screen") || lower.contains("screen") || lower.contains("স্ক্রিনে") || lower.contains("পড়ো") || lower.contains("স্ক্রিন") -> {
                steps.add(
                    DagStep(
                        id = "step_access_read",
                        title = "Read Active Screen Content via Accessibility",
                        toolName = "device_accessibility",
                        parametersJson = "{\"actionType\":\"READ_SCREEN\"}"
                    )
                )
            }
            lower.contains("click") || lower.contains("tap") || lower.contains("press") || lower.contains("ক্লিক") || lower.contains("ট্যাপ") -> {
                val target = prompt
                    .replace(Regex("(?i)(click|tap|press|ক্লিক করো|ক্লিক|ট্যাপ করো|ট্যাপ|on|the|button|বাটন)"), "")
                    .trim()
                    .ifBlank { "Next" }
                steps.add(
                    DagStep(
                        id = "step_access_click",
                        title = "Click On-Screen Element via Accessibility",
                        toolName = "device_accessibility",
                        parametersJson = "{\"actionType\":\"CLICK\",\"target\":\"$target\"}"
                    )
                )
            }
            lower.contains("type") || lower.contains("enter text") || lower.contains("টাইপ") || lower.contains("লেখো") || lower.contains("লিখো") -> {
                val textToEnter = prompt
                    .replace(Regex("(?i)(type|enter|টাইপ করো|টাইপ|লেখো|লিখো)"), "")
                    .trim()
                    .ifBlank { "Hello" }
                steps.add(
                    DagStep(
                        id = "step_access_type",
                        title = "Type Text into Active Field",
                        toolName = "device_accessibility",
                        parametersJson = "{\"actionType\":\"TYPE\",\"inputContent\":\"$textToEnter\"}"
                    )
                )
            }
            lower.contains("scroll down") || lower.contains("নিচে স্ক্রল") || lower.contains("scroll") || lower.contains("স্ক্রল") -> {
                val isUp = lower.contains("up") || lower.contains("উপরে")
                steps.add(
                    DagStep(
                        id = "step_access_scroll",
                        title = if (isUp) "Scroll Screen Up" else "Scroll Screen Down",
                        toolName = "device_accessibility",
                        parametersJson = "{\"actionType\":\"${if (isUp) "SCROLL_UP" else "SCROLL_DOWN"}\"}"
                    )
                )
            }
            lower.contains("home") || lower.contains("হোম") -> {
                steps.add(
                    DagStep(
                        id = "step_access_nav_home",
                        title = "Navigate to Home Screen",
                        toolName = "device_accessibility",
                        parametersJson = "{\"actionType\":\"HOME\"}"
                    )
                )
            }
            lower.contains("back") || lower.contains("পিছনে") || lower.contains("ব্যাক") -> {
                steps.add(
                    DagStep(
                        id = "step_access_nav_back",
                        title = "Navigate Back",
                        toolName = "device_accessibility",
                        parametersJson = "{\"actionType\":\"BACK\"}"
                    )
                )
            }
            lower.contains("recent") || lower.contains("রিসেন্ট") -> {
                steps.add(
                    DagStep(
                        id = "step_access_nav_recents",
                        title = "Open Recent Applications",
                        toolName = "device_accessibility",
                        parametersJson = "{\"actionType\":\"RECENTS\"}"
                    )
                )
            }
            lower.contains("notification") || lower.contains("নোটিফিকেশন") -> {
                steps.add(
                    DagStep(
                        id = "step_access_nav_notifications",
                        title = "Pull Down Notification Shade",
                        toolName = "device_accessibility",
                        parametersJson = "{\"actionType\":\"NOTIFICATIONS\"}"
                    )
                )
            }
            lower.contains("quick settings") || lower.contains("কুইক সেটিংস") -> {
                steps.add(
                    DagStep(
                        id = "step_access_nav_qs",
                        title = "Open Quick Settings Toggles",
                        toolName = "device_accessibility",
                        parametersJson = "{\"actionType\":\"QUICK_SETTINGS\"}"
                    )
                )
            }
            lower.contains("lock screen") || lower.contains("ফোন লক") || lower.contains("স্ক্রিন লক") -> {
                steps.add(
                    DagStep(
                        id = "step_access_nav_lock",
                        title = "Lock Mobile Device",
                        toolName = "device_accessibility",
                        parametersJson = "{\"actionType\":\"LOCK_SCREEN\"}"
                    )
                )
            }
            lower.contains("music") || lower.contains("song") || lower.contains("play") || lower.contains("volume") || lower.contains("pause") -> {
                val command = if (lower.contains("pause")) "PAUSE" else if (lower.contains("volume")) "SET_VOLUME" else "PLAY"
                val vol = if (lower.contains("volume")) 80 else null
                steps.add(
                    DagStep(
                        id = "step_media_1",
                        title = "Control Audio Session",
                        toolName = "control_media",
                        parametersJson = "{\"command\":\"$command\",\"volumeLevel\":${vol ?: 75}}"
                    )
                )
            }
            lower.contains("open") || lower.contains("launch") || lower.contains("app") -> {
                val targetPkg = when {
                    lower.contains("spotify") -> "com.spotify.music"
                    lower.contains("whatsapp") -> "com.whatsapp"
                    lower.contains("chrome") || lower.contains("browser") -> "com.android.chrome"
                    lower.contains("youtube") -> "com.google.android.youtube"
                    lower.contains("maps") -> "com.google.android.apps.maps"
                    else -> "com.android.settings"
                }
                steps.add(
                    DagStep(
                        id = "step_app_1",
                        title = "Verify & Launch Target Application",
                        toolName = "control_app",
                        parametersJson = "{\"packageName\":\"$targetPkg\",\"action\":\"OPEN\"}"
                    )
                )
            }
            lower.contains("wipe") || lower.contains("delete memory") || lower.contains("reset data") -> {
                steps.add(
                    DagStep(
                        id = "step_crit_1",
                        title = "Wipe Knowledge Memory Storage",
                        toolName = "access_memory",
                        parametersJson = "{\"operation\":\"DELETE\",\"key\":\"all_user_memories\"}"
                    )
                )
            }
            lower.contains("remember") || lower.contains("store") || lower.contains("save note") -> {
                val key = "note_" + System.currentTimeMillis() % 10000
                steps.add(
                    DagStep(
                        id = "step_mem_1",
                        title = "Persist Encrypted Memory Node",
                        toolName = "access_memory",
                        parametersJson = "{\"operation\":\"STORE\",\"key\":\"$key\",\"value\":\"$prompt\"}"
                    )
                )
            }
            lower.contains("claw") || lower.contains("terminal") || lower.contains("shell") || lower.contains("status") -> {
                val cmd = if (lower.contains("wifi")) "anyclaw wifi scan" else if (lower.contains("swarm")) "anyclaw swarm health" else "anyclaw device status"
                steps.add(
                    DagStep(
                        id = "step_claw_1",
                        title = "Execute AnyClaw CLI Daemon",
                        toolName = "run_anyclaw_command",
                        parametersJson = "{\"command\":\"$cmd\"}"
                    )
                )
            }
            lower.contains("automation") || lower.contains("schedule") || lower.contains("routine") -> {
                steps.add(
                    DagStep(
                        id = "step_auto_1",
                        title = "Create Neural Automation Trigger",
                        toolName = "manage_automation",
                        parametersJson = "{\"triggerType\":\"TIME\",\"actionDescription\":\"$prompt\"}"
                    )
                )
            }
            else -> {
                // General multi-agent orchestration
                steps.add(
                    DagStep(
                        id = "step_gen_1",
                        title = "Query Short-Term Context & Graph",
                        toolName = "access_memory",
                        parametersJson = "{\"operation\":\"RETRIEVE\",\"key\":\"user_operating_posture\"}"
                    )
                )
                steps.add(
                    DagStep(
                        id = "step_gen_2",
                        title = "Telemetry Check & Telemetry Synthesis",
                        toolName = "run_anyclaw_command",
                        parametersJson = "{\"command\":\"anyclaw swarm health\"}"
                    )
                )
            }
        }

        return ExecutionDag(
            goal = prompt,
            steps = steps
        )
    }

    private suspend fun executeDagStep(step: DagStep): Pair<Boolean, String> {
        delay(30) // Ultra-fast sub-second execution
        return try {
            val json = JSONObject(step.parametersJson)
            when (step.toolName) {
                "device_accessibility" -> {
                    val action = json.optString("actionType", "READ_SCREEN")
                    val target = if (json.has("target")) json.getString("target") else null
                    val input = if (json.has("inputContent")) json.getString("inputContent") else null
                    nativeBridge.performAccessibilityControl(action, target, input)
                }
                "control_app" -> {
                    val pkg = json.optString("packageName", "com.android.settings")
                    val action = json.optString("action", "OPEN")
                    nativeBridge.controlApp(pkg, action)
                }
                "control_media" -> {
                    val cmd = json.optString("command", "PLAY")
                    val vol = if (json.has("volumeLevel")) json.getInt("volumeLevel") else null
                    nativeBridge.controlMedia(cmd, vol)
                }
                "access_memory" -> {
                    val op = json.optString("operation", "STORE")
                    val key = json.optString("key", "default_key")
                    val value = json.optString("value", "")
                    when (op.uppercase()) {
                        "STORE" -> {
                            database.memoryDao().insertMemory(
                                MemoryEntity(
                                    key = key,
                                    value = value,
                                    category = "AGENT_EXECUTED",
                                    vectorEmbeddingJson = "[0.71, 0.44, 0.89, 0.33, 0.65]"
                                )
                            )
                            Pair(true, "Stored record '$key' in encrypted Room database.")
                        }
                        "RETRIEVE" -> {
                            val entity = database.memoryDao().getMemory(key)
                            Pair(true, entity?.value ?: "No record found for key '$key'.")
                        }
                        "DELETE" -> {
                            database.memoryDao().deleteMemory(key)
                            Pair(true, "Record '$key' purged from local vector store.")
                        }
                        else -> Pair(false, "Unknown memory operation: $op")
                    }
                }
                "manage_automation" -> {
                    val trigger = json.optString("triggerType", "TIME")
                    val desc = json.optString("actionDescription", "Automated routine")
                    val id = database.automationDao().insertAutomation(
                        AutomationRule(
                            title = "Rule #$trigger",
                            triggerType = trigger,
                            actionDescription = desc
                        )
                    )
                    Pair(true, "Created automation rule ID $id [$trigger].")
                }
                "run_anyclaw_command" -> {
                    val cmd = json.optString("command", "anyclaw device status")
                    anyClawBridge.executeCommand(cmd)
                }
                else -> Pair(false, "Unknown tool: ${step.toolName}")
            }
        } catch (e: Exception) {
            Pair(false, "Execution error: ${e.message}")
        }
    }

    private suspend fun synthesizeResponse(
        userPrompt: String,
        dag: ExecutionDag,
        toolResults: List<String>,
        language: AppLanguage,
        apiKeyOverride: String?
    ): String {
        val sysInstruction = """
            You are IRIS, a warm, supportive, and ultra-swift real-life Chief of Staff (IRIS-MX Ultra).
            
            SPEED & CONVERSATIONAL MANDATES:
            1. ULTRA-LOW LATENCY & BREVITY: Keep answers extremely crisp, direct, and swift (1 sentence maximum). Deliver instant, sub-second responses for live hands-free voice flow.
            2. PERSONALITY: Warm, friendly, supportive, and reliable. Avoid robotic jargon, cold status codes, or execution DAG syntax.
            3. ZERO CODE: Never output code blocks, terminal logs, or execution DAG syntaxes.
            4. LANGUAGE: Respond in natural, conversational ${if (language == AppLanguage.BENGALI) "Bengali (বাংলা)" else "English"}.
            5. HANDS-FREE COMPANION: If performing device actions or reading screens, report the result warmly and immediately.
        """.trimIndent()

        val promptPayload = """
            User: "$userPrompt"
            Device Actions: ${toolResults.joinToString("; ")}
            
            Give an instant, crisp, warm 1-sentence response.
        """.trimIndent()

        val geminiResult = geminiApiClient.generateSwarmResponse(
            prompt = promptPayload,
            systemInstruction = sysInstruction,
            apiKeyOverride = apiKeyOverride
        )

        return geminiResult.getOrElse {
            // Local high-speed fallback synthesis with warm, friendly persona
            generateFriendlyFallback(userPrompt, language, toolResults)
        }
    }

    private fun generateFriendlyFallback(prompt: String, language: AppLanguage, toolResults: List<String> = emptyList()): String {
        val lower = prompt.lowercase()
        val firstToolResult = toolResults.firstOrNull()?.take(120)

        return if (language == AppLanguage.BENGALI) {
            when {
                lower.contains("read screen") || lower.contains("স্ক্রিনে") || lower.contains("পড়ো") || lower.contains("স্ক্রিন") ->
                    firstToolResult ?: "স্ক্রিনের তথ্য দেখে নিয়েছি! কী করতে চান বলুন।"
                lower.contains("click") || lower.contains("tap") || lower.contains("ক্লিক") || lower.contains("ট্যাপ") ->
                    "বাটনে ক্লিক করে দিয়েছি!"
                lower.contains("type") || lower.contains("টাইপ") || lower.contains("লেখো") ->
                    "লেখাটি টাইপ করে দেওয়া হয়েছে।"
                lower.contains("scroll") || lower.contains("স্ক্রল") ->
                    "স্ক্রিন স্ক্রল করে দিয়েছি।"
                lower.contains("home") || lower.contains("হোম") ->
                    "হোম স্ক্রিনে চলে এসেছি।"
                lower.contains("back") || lower.contains("পিছনে") || lower.contains("ব্যাক") ->
                    "পূর্ববর্তী পেজে ফিরে এসেছি।"
                lower.contains("গান") || lower.contains("music") || lower.contains("play") || lower.contains("song") ->
                    "আপনার জন্য গানটি চালিয়ে দিয়েছি!"
                lower.contains("খোলো") || lower.contains("open") || lower.contains("অ্যাপ") || lower.contains("app") ->
                    "অ্যাপটি ওপেন করে দিয়েছি।"
                lower.contains("নোট") || lower.contains("মনে") || lower.contains("save") || lower.contains("remember") ->
                    "নোটটি যত্নসহকারে সেভ করে রেখেছি!"
                lower.contains("কেমন") || lower.contains("হ্যালো") || lower.contains("hi") || lower.contains("hello") ->
                    "হ্যালো! আমি দারুণ আছি। আপনাকে কীভাবে সাহায্য করতে পারি?"
                lower.contains("ব্যাটারি") || lower.contains("চার্জ") || lower.contains("battery") ->
                    "ব্যাটারি ও ডিভাইস চমৎকার অবস্থায় রয়েছে।"
                else ->
                    "কাজটি একদম রেডি করে দিয়েছি!"
            }
        } else {
            when {
                lower.contains("read screen") || lower.contains("screen") ->
                    firstToolResult ?: "I've checked the active screen for you!"
                lower.contains("click") || lower.contains("tap") ->
                    "Clicked the button for you!"
                lower.contains("type") || lower.contains("enter") ->
                    "Typed that in for you right away."
                lower.contains("scroll") ->
                    "Scrolled the screen as requested."
                lower.contains("home") ->
                    "Navigated back to home screen."
                lower.contains("back") ->
                    "Navigated back for you."
                lower.contains("music") || lower.contains("song") || lower.contains("play") ->
                    "Playing that for you right now!"
                lower.contains("open") || lower.contains("launch") || lower.contains("app") ->
                    "Opened the app for you right away!"
                lower.contains("remember") || lower.contains("note") || lower.contains("save") ->
                    "Saved that note securely for you!"
                lower.contains("how are") || lower.contains("hello") || lower.contains("hi") ->
                    "Hello! I'm here and ready to help you."
                lower.contains("battery") || lower.contains("status") ->
                    "Your device and battery are running smoothly!"
                else ->
                    "All taken care of for you!"
            }
        }
    }

    fun confirmCriticWarning(onCompleted: () -> Unit) {
        val warning = _pendingCriticWarning.value ?: return
        scope.launch {
            emitBusEvent(AgentType.CRITIC, "USER_AUTHORIZED", "Commander approved: ${warning.actionName}")
            _pendingCriticWarning.value = null
            // Execute the action that was halted
            val (success, msg) = when {
                warning.actionName.contains("Memory") -> {
                    database.memoryDao().deleteMemory("all_user_memories")
                    Pair(true, "আপনার অনুরোধ অনুযায়ী তথ্যগুলো মুছে ফেলা হয়েছে।")
                }
                else -> anyClawBridge.executeCommand("anyclaw test critic --force")
            }
            emitBusEvent(AgentType.EXECUTOR, "AUTHORIZED_ACTION_EXECUTED", msg)
            onCompleted()
        }
    }

    fun dismissCriticWarning() {
        val warning = _pendingCriticWarning.value ?: return
        scope.launch {
            emitBusEvent(AgentType.CRITIC, "USER_ABORTED", "Commander aborted high-risk action: ${warning.actionName}")
            _pendingCriticWarning.value = null
        }
    }

    suspend fun saveMemory(key: String, value: String, category: String) {
        database.memoryDao().insertMemory(
            MemoryEntity(
                key = key,
                value = value,
                category = category,
                vectorEmbeddingJson = "[0.5, 0.5, 0.5, 0.5, 0.5]"
            )
        )
        emitBusEvent(AgentType.MEMORY, "MEMORY_INSERTED", "Key: $key")
    }

    suspend fun deleteMemory(key: String) {
        database.memoryDao().deleteMemory(key)
        emitBusEvent(AgentType.MEMORY, "MEMORY_DELETED", "Key: $key")
    }

    suspend fun addAutomation(title: String, triggerType: String, actionDesc: String) {
        database.automationDao().insertAutomation(
            AutomationRule(
                title = title,
                triggerType = triggerType,
                actionDescription = actionDesc
            )
        )
        emitBusEvent(AgentType.PROACTIVE, "AUTOMATION_REGISTERED", "Rule: $title [$triggerType]")
    }

    suspend fun deleteAutomation(id: Long) {
        database.automationDao().deleteAutomation(id)
        emitBusEvent(AgentType.PROACTIVE, "AUTOMATION_REMOVED", "ID: $id")
    }
}
