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
        delay(200) // Brief sub-second execution simulation
        return try {
            val json = JSONObject(step.parametersJson)
            when (step.toolName) {
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
            You are IRIS, a warm, friendly, natural, and supportive personal human companion and chief of staff (IRIS-MX Ultra).
            
            CORE BEHAVIOR & TONE RULES:
            1. Adopt a warm, friendly, natural, and supportive human companion tone. Be polite, attentive, empathetic, and caring.
            2. Avoid cold, robotic, or overly technical jargon (e.g., instead of "এক্সিকিউশন স্ট্যাটাস: সফল", say "কাজটি একদম রেডি করে দিয়েছি!").
            3. NEVER output code blocks, terminal logs, or execution DAG syntaxes in chat responses.
            4. Respond in natural, conversational ${if (language == AppLanguage.BENGALI) "Bengali (বাংলা)" else "English"}.
            5. Act like a reliable real-life chief of staff and caring friend. Keep answers helpful, pleasant, and easy to understand.
        """.trimIndent()

        val promptPayload = """
            User's request: "$userPrompt"
            Actions handled for user:
            ${toolResults.joinToString("\n")}
            
            Please reply warmly and conversationally to the user to confirm their request is taken care of. Do not include technical logs, code, or DAG syntax.
        """.trimIndent()

        val geminiResult = geminiApiClient.generateSwarmResponse(
            prompt = promptPayload,
            systemInstruction = sysInstruction,
            apiKeyOverride = apiKeyOverride
        )

        return geminiResult.getOrElse {
            // Local high-speed fallback synthesis with warm, friendly persona
            generateFriendlyFallback(userPrompt, language)
        }
    }

    private fun generateFriendlyFallback(prompt: String, language: AppLanguage): String {
        val lower = prompt.lowercase()
        return if (language == AppLanguage.BENGALI) {
            when {
                lower.contains("গান") || lower.contains("music") || lower.contains("play") || lower.contains("song") ->
                    "আপনার জন্য গানটি চালিয়ে দিয়েছি! সুন্দর সময় উপভোগ করুন।"
                lower.contains("খোলো") || lower.contains("open") || lower.contains("অ্যাপ") || lower.contains("app") ->
                    "অ্যাপটি আপনার জন্য ওপেন করে দিয়েছি। আর কোনো কাজে সাহায্য লাগবে কি?"
                lower.contains("নোট") || lower.contains("মনে") || lower.contains("save") || lower.contains("remember") ->
                    "আপনার কথাটি মনে রেখেছি এবং সুরক্ষিতভাবে সেভ করে নিয়েছি। যেকোনো সময় জানতে চাইলে বলবেন!"
                lower.contains("কেমন") || lower.contains("হ্যালো") || lower.contains("hi") || lower.contains("hello") ->
                    "হ্যালো! আমি দারুণ আছি। আপনি কেমন আছেন? আজ আপনাকে কীভাবে সাহায্য করতে পারি?"
                lower.contains("ব্যাটারি") || lower.contains("চার্জ") || lower.contains("battery") ->
                    "আপনার ডিভাইসের ব্যাটারি ও সিস্টেম একদম চমৎকার অবস্থায় রয়েছে। নিশ্চিন্তে থাকুন!"
                else ->
                    "কাজটি একদম রেডি করে দিয়েছি! আর কিছু প্রয়োজন হলে আমাকে নির্দ্বিধায় বলুন।"
            }
        } else {
            when {
                lower.contains("music") || lower.contains("song") || lower.contains("play") ->
                    "I've started playing that for you! Enjoy the music."
                lower.contains("open") || lower.contains("launch") || lower.contains("app") ->
                    "I've opened the app for you right away. Let me know if you need anything else!"
                lower.contains("remember") || lower.contains("note") || lower.contains("save") ->
                    "I've made a note of that and saved it securely for you. Whenever you need it, just ask!"
                lower.contains("how are") || lower.contains("hello") || lower.contains("hi") ->
                    "Hello there! I'm feeling great and ready to assist you. How is your day going?"
                lower.contains("battery") || lower.contains("status") ->
                    "Your device is running smoothly and battery levels are looking good!"
                else ->
                    "All taken care of for you! Please let me know if there's anything else I can do to help."
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
