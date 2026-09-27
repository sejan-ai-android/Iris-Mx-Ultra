package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.IrisViewModel

enum class CodePlatform {
    TYPESCRIPT_REACT_NATIVE,
    KOTLIN_ANDROID_NATIVE
}

data class CodeFileItem(
    val fileName: String,
    val description: String,
    val language: String,
    val content: String
)

@Composable
fun CodeGenScreen(
    viewModel: IrisViewModel,
    modifier: Modifier = Modifier
) {
    var selectedPlatform by remember { mutableStateOf(CodePlatform.TYPESCRIPT_REACT_NATIVE) }
    var selectedFileIndex by remember { mutableIntStateOf(0) }
    var customPrompt by remember { mutableStateOf("") }
    var showArchitectureBlueprint by remember { mutableStateOf(false) }
    var copyStatusMessage by remember { mutableStateOf<String?>(null) }

    val tsFiles = remember { getProductionTypeScriptFiles() }
    val ktFiles = remember { getProductionKotlinFiles() }

    val currentFiles = if (selectedPlatform == CodePlatform.TYPESCRIPT_REACT_NATIVE) tsFiles else ktFiles
    val activeFile = currentFiles.getOrNull(selectedFileIndex) ?: currentFiles.first()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IrisBackground)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "CODE GENERATION ENGINE",
                    color = IrisCyanPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Production Swarm & Native Implementations",
                    color = IrisTextMuted,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = { showArchitectureBlueprint = true },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(IrisSurfaceElevated)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = "Architecture Blueprint",
                    tint = IrisCyanPrimary
                )
            }
        }

        // Platform Switcher (TypeScript / React Native vs Kotlin / Android Native)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabButton(
                title = "TypeScript / React Native",
                subtitle = "Expo • Zustand • MMKV",
                isSelected = selectedPlatform == CodePlatform.TYPESCRIPT_REACT_NATIVE,
                onClick = {
                    selectedPlatform = CodePlatform.TYPESCRIPT_REACT_NATIVE
                    selectedFileIndex = 0
                },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                title = "Kotlin / Android Native",
                subtitle = "Jetpack Compose • Room",
                isSelected = selectedPlatform == CodePlatform.KOTLIN_ANDROID_NATIVE,
                onClick = {
                    selectedPlatform = CodePlatform.KOTLIN_ANDROID_NATIVE
                    selectedFileIndex = 0
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Horizontal File Selector
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(currentFiles.indices.toList()) { index ->
                val file = currentFiles[index]
                val isSelected = selectedFileIndex == index
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFileIndex = index },
                    label = { Text(file.fileName, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IrisCyanPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = IrisCyanPrimary,
                        containerColor = IrisSurfaceVariant,
                        labelColor = IrisTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) IrisCyanPrimary else IrisBorder
                    )
                )
            }
        }

        // Active File Meta & Copy Action Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            color = IrisSurfaceElevated,
            shape = RoundedCornerShape(8.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(IrisBorder)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeFile.fileName,
                        color = IrisTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = activeFile.description,
                        color = IrisTextMuted,
                        fontSize = 10.sp
                    )
                }

                Button(
                    onClick = {
                        viewModel.nativeBridge.copyToClipboard(activeFile.fileName, activeFile.content)
                        copyStatusMessage = "Copied ${activeFile.fileName} to clipboard!"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IrisCyanPrimary,
                        contentColor = IrisBackground
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("copy_code_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy Code", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Code Viewer Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(IrisSurface)
                .border(1.dp, IrisBorder, RoundedCornerShape(8.dp))
        ) {
            val horizontalScroll = rememberScrollState()
            val verticalScroll = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScroll)
                    .horizontalScroll(horizontalScroll)
                    .padding(12.dp)
            ) {
                val lines = activeFile.content.lines()
                lines.forEachIndexed { i, line ->
                    Row {
                        Text(
                            text = "${(i + 1).toString().padStart(3, ' ')}  ",
                            color = IrisTextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        Text(
                            text = line,
                            color = when {
                                line.trim().startsWith("//") || line.trim().startsWith("/*") -> IrisEmeraldAccent
                                line.trim().startsWith("import") || line.trim().startsWith("export") || line.trim().startsWith("package") -> IrisVioletSecondary
                                line.contains("class") || line.contains("interface") || line.contains("const") || line.contains("val") -> IrisCyanPrimary
                                else -> IrisTextPrimary
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Custom Code Generator Prompt Box
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = IrisSurfaceVariant
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "NEURAL CODE GENERATOR:",
                    color = IrisTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = customPrompt,
                        onValueChange = { customPrompt = it },
                        placeholder = {
                            Text(
                                "e.g. Generate AnyClaw camera frame bridge in TypeScript...",
                                color = IrisTextMuted,
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = IrisSurfaceElevated,
                            unfocusedContainerColor = IrisSurfaceElevated,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = IrisTextPrimary,
                            unfocusedTextColor = IrisTextPrimary
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (customPrompt.isNotBlank()) {
                                viewModel.submitPrompt("Generate production code implementation: $customPrompt")
                                viewModel.setTab(com.example.viewmodel.IrisNavigationTab.ORCHESTRATION)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IrisVioletSecondary,
                            contentColor = IrisTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Generate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Architecture Blueprint Dialog
    if (showArchitectureBlueprint) {
        AlertDialog(
            onDismissRequest = { showArchitectureBlueprint = false },
            title = {
                Text(
                    "IRIS-MX ULTRA ARCHITECTURE",
                    color = IrisCyanPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Shared Internal Event Bus Matrix:",
                        color = IrisTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "1. Orchestrator -> Central router with sub-second latency budget.\n" +
                                "2. PlannerAgent -> Step DAG generator with tool bindings.\n" +
                                "3. ExecutorAgent -> Native Android bridges & AnyClaw CLI daemon.\n" +
                                "4. MemoryAgent -> Zustand short-term, MMKV/Room long-term, vector cosine search.\n" +
                                "5. CriticAgent -> Mandatory safety interceptor (high-risk guardrail).\n" +
                                "6. ProactiveAgent -> Hardware telemetry & contextual triggers.\n" +
                                "7. EmotionAgent -> Sentiment posture & executive voice modulation.",
                        color = IrisTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showArchitectureBlueprint = false },
                    colors = ButtonDefaults.buttonColors(containerColor = IrisCyanPrimary, contentColor = IrisBackground)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = IrisSurfaceElevated
        )
    }
}

@Composable
fun TabButton(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, if (isSelected) IrisCyanPrimary else IrisBorder, RoundedCornerShape(8.dp)),
        color = if (isSelected) IrisCyanPrimary.copy(alpha = 0.15f) else IrisSurfaceVariant,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                text = title,
                color = if (isSelected) IrisCyanPrimary else IrisTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = IrisTextMuted,
                fontSize = 9.sp
            )
        }
    }
}

fun getProductionTypeScriptFiles(): List<CodeFileItem> {
    return listOf(
        CodeFileItem(
            fileName = "IrisMultiAgentSwarm.ts",
            description = "7-Agent Orchestrator, DAG planner, and event bus",
            language = "typescript",
            content = """
// IRIS-MX ULTRA: Multi-Agent Swarm Orchestrator (TypeScript/React Native)
// Package: com.irismx.ultra | Version 1.0.0

import { EventEmitter } from 'events';
import { mmkvStorage } from './storage';

export type AgentRole = 
  | 'Orchestrator' 
  | 'PlannerAgent' 
  | 'ExecutorAgent' 
  | 'MemoryAgent' 
  | 'CriticAgent' 
  | 'ProactiveAgent' 
  | 'EmotionAgent';

export interface ExecutionDagStep {
  id: string;
  tool: string;
  parameters: Record<string, unknown>;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'INTERCEPTED' | 'FAILED';
}

export class IrisSwarmBus extends EventEmitter {
  private static instance: IrisSwarmBus;
  private latencyBudgetMs: number = 850;

  private constructor() {
    super();
  }

  public static getInstance(): IrisSwarmBus {
    if (!IrisSwarmBus.instance) {
      IrisSwarmBus.instance = new IrisSwarmBus();
    }
    return IrisSwarmBus.instance;
  }

  public dispatch(agent: AgentRole, event: string, payload: unknown): void {
    const timestamp = Date.now();
    this.emit('AGENT_EVENT', { agent, event, payload, timestamp });
  }
}

export class OrchestratorAgent {
  private bus = IrisSwarmBus.getInstance();

  async processIntent(prompt: string): Promise<string> {
    const startTime = Date.now();
    this.bus.dispatch('Orchestrator', 'INTENT_PARSED', { prompt });

    // Step 1: Critic Intercept Check
    if (this.isDestructive(prompt)) {
      this.bus.dispatch('CriticAgent', 'INTERCEPT_RAISED', { prompt });
      throw new Error('CRITIC_INTERCEPT_REQUIRED: Operation involves irreversible actions.');
    }

    // Step 2: Executor Tool Binding
    this.bus.dispatch('ExecutorAgent', 'TASK_EXECUTED', { status: 'SUCCESS' });
    const latency = Date.now() - startTime;
    return `[IRIS-MX TS] Executed with latency: ` + latency + `ms`;
  }

  private isDestructive(prompt: string): boolean {
    const keywords = ['delete', 'wipe', 'reset', 'transfer', 'drop'];
    return keywords.some(k => prompt.toLowerCase().includes(k));
  }
}
            """.trimIndent()
        ),
        CodeFileItem(
            fileName = "AnyClawBridge.ts",
            description = "UNIX domain socket & terminal bridge to AnyClaw daemon",
            language = "typescript",
            content = """
// AnyClaw Terminal Integration Bridge
// Communicates with native /dev/socket/iris_claw daemon

export interface AnyClawResponse {
  exitCode: number;
  stdout: string;
  stderr: string;
  latencyMs: number;
}

export class AnyClawBridge {
  private static socketPath = '/dev/socket/iris_claw';

  static async executeCommand(command: string): Promise<AnyClawResponse> {
    const start = Date.now();
    // Native bridge invocation via TurboModule / JSI
    const res = await (global as any).IrisNativeBridge?.executeAnyClaw?.(command) ?? {
      exitCode: 0,
      stdout: '[ANYCLAW SHIM] Executed: ' + command + ' on ' + AnyClawBridge.socketPath,
      stderr: ''
    };

    return {
      ...res,
      latencyMs: Date.now() - start
    };
  }
}
            """.trimIndent()
        ),
        CodeFileItem(
            fileName = "CriticSafetyInterceptor.ts",
            description = "MANDATORY safety interceptor for financial, delete, & system calls",
            language = "typescript",
            content = """
// CriticAgent: Mandatory Safety Interceptor & Policy Enforcer

export interface SafetyValidationResult {
  isSafe: boolean;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  rationale: string;
  requiresBiometricConfirmation: boolean;
}

export class CriticSafetyInterceptor {
  static evaluate(actionName: string, parameters: Record<string, unknown>): SafetyValidationResult {
    const paramStr = JSON.stringify(parameters).toLowerCase();
    
    if (paramStr.includes('delete') || paramStr.includes('wipe') || actionName.includes('WIPE')) {
      return {
        isSafe: false,
        riskLevel: 'CRITICAL',
        rationale: 'Destructive storage deletion intercepted. Mandatory executive sign-off.',
        requiresBiometricConfirmation: true
      };
    }

    if (actionName.includes('FINANCIAL') || paramStr.includes('transfer')) {
      return {
        isSafe: false,
        riskLevel: 'HIGH',
        rationale: 'Financial routing call detected.',
        requiresBiometricConfirmation: true
      };
    }

    return {
      isSafe: true,
      riskLevel: 'LOW',
      rationale: 'Action policy compliant.',
      requiresBiometricConfirmation: false
    };
  }
}
            """.trimIndent()
        )
    )
}

fun getProductionKotlinFiles(): List<CodeFileItem> {
    return listOf(
        CodeFileItem(
            fileName = "IrisOrchestrator.kt",
            description = "Kotlin native multi-agent swarm router & event bus",
            language = "kotlin",
            content = """
package com.irismx.ultra.core

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IrisOrchestrator(
    private val criticValidator: SafetyCriticValidator,
    private val nativeBridge: NativeDeviceBridge,
    private val eventBus: MutableSharedFlow<AgentEventLog>
) {
    suspend fun processGoal(prompt: String): ExecutionResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        eventBus.emit(AgentEventLog(agentType = "Orchestrator", eventType = "START", message = prompt))

        // Intercept high risk
        val safetyCheck = criticValidator.validate(prompt)
        if (!safetyCheck.isApproved) {
            eventBus.emit(AgentEventLog(agentType = "CriticAgent", eventType = "HALTED", message = safetyCheck.reason))
            return@withContext ExecutionResult.Intercepted(safetyCheck.reason)
        }

        // Execute natively
        val result = nativeBridge.dispatchTool(prompt)
        val latency = System.currentTimeMillis() - start
        ExecutionResult.Success(result, latency)
    }
}

sealed class ExecutionResult {
    data class Success(val output: String, val latencyMs: Long) : ExecutionResult()
    data class Intercepted(val reason: String) : ExecutionResult()
}
            """.trimIndent()
        ),
        CodeFileItem(
            fileName = "NativeDeviceBridge.kt",
            description = "Android system audio, package manager, and battery bridges",
            language = "kotlin",
            content = """
package com.irismx.ultra.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.BatteryManager

class NativeDeviceBridge(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun controlMedia(command: String, volumePercent: Int?) {
        when (command) {
            "SET_VOLUME" -> {
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val target = ((volumePercent ?: 50) * max) / 100
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
            }
        }
    }

    fun launchApp(packageName: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return true
    }
}
            """.trimIndent()
        ),
        CodeFileItem(
            fileName = "SafetyCriticValidator.kt",
            description = "Level-4 safety interceptor for Android Native runtime",
            language = "kotlin",
            content = """
package com.irismx.ultra.safety

data class CriticVerdict(
    val isApproved: Boolean,
    val riskLevel: String,
    val reason: String
)

class SafetyCriticValidator {
    fun validate(commandPayload: String): CriticVerdict {
        val lower = commandPayload.lowercase()
        return if (lower.contains("wipe") || lower.contains("delete") || lower.contains("reset")) {
            CriticVerdict(
                isApproved = false,
                riskLevel = "CRITICAL",
                reason = "CriticAgent Shield: Destructive operation detected. Authorization required."
            )
        } else {
            CriticVerdict(isApproved = true, riskLevel = "LOW", reason = "Policy verified.")
        }
    }
}
            """.trimIndent()
        )
    )
}
