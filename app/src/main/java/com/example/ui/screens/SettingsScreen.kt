package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import com.example.data.model.AppLanguage
import com.example.ui.theme.*
import com.example.viewmodel.IrisViewModel

@Composable
fun SettingsScreen(
    viewModel: IrisViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.appLanguage.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    val autoSpeak by viewModel.autoSpeakResponse.collectAsState()
    val recentLogs by viewModel.recentLogs.collectAsState()

    var showKeyDialog by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf(customApiKey) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IrisBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = "NEURAL CORE SETTINGS",
                color = IrisCyanPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "IRIS-MX Ultra System Preferences & Security",
                color = IrisTextMuted,
                fontSize = 11.sp
            )
        }

        // Section: Language Mode
        SettingSection(title = "LANGUAGE & POSTURE") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LanguageOptionCard(
                    title = "English (Executive)",
                    subtitle = "Sub-second, zero fluff, concise directives",
                    isSelected = language == AppLanguage.ENGLISH,
                    onClick = { viewModel.setLanguage(AppLanguage.ENGLISH) },
                    modifier = Modifier.weight(1f)
                )
                LanguageOptionCard(
                    title = "বাংলা (নির্বাহী)",
                    subtitle = "স্বাভাবিক বাংলা ভয়েস ও স্বয়ংক্রিয় সোয়ার্ম",
                    isSelected = language == AppLanguage.BENGALI,
                    onClick = { viewModel.setLanguage(AppLanguage.BENGALI) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section: Voice & Speech Pipeline
        SettingSection(title = "VOICE & NEURAL TTS PIPELINE") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp)),
                color = IrisSurfaceElevated
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Speak Swarm Responses",
                            color = IrisTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Play response audio via local TextToSpeech engine automatically",
                            color = IrisTextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = autoSpeak,
                        onCheckedChange = { viewModel.toggleAutoSpeak() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = IrisBackground,
                            checkedTrackColor = IrisCyanPrimary
                        )
                    )
                }
            }
        }

        // Section: Gemini Cloud API Key
        SettingSection(title = "GEMINI 3.5 FLASH CLOUD INTEGRATION") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, IrisBorder, RoundedCornerShape(10.dp)),
                color = IrisSurfaceElevated
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = "API Key",
                            tint = IrisCyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "API Key Status:",
                            color = IrisTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (customApiKey.isNotBlank()) "ACTIVE OVERRIDE" else "SYSTEM INJECTED / OFFLINE FALLBACK",
                            color = IrisEmeraldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Configured via AI Studio Secrets panel. A local high-speed neural engine automatically executes Swarm DAGs if offline.",
                        color = IrisTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            keyInput = customApiKey
                            showKeyDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IrisSurfaceVariant, contentColor = IrisCyanPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("configure_api_key_button")
                    ) {
                        Text("Configure Custom Key", fontSize = 11.sp)
                    }
                }
            }
        }

        // Section: CriticAgent Safety & AnyClaw Diagnostics
        SettingSection(title = "CRITIC-AGENT SAFETY & DIAGNOSTICS") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp)),
                color = IrisSurfaceElevated
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Shield, contentDescription = "Shield", tint = IrisAlertRed, modifier = Modifier.size(18.dp))
                        Text("Critic Interceptor Policy Level: 4", color = IrisTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Destructive and sensitive operations (app wipes, command executions, system triggers) are intercepted and require explicit Commander approval.",
                        color = IrisTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Button(
                        onClick = {
                            viewModel.submitPrompt("Wipe memory cache (Test Critic)")
                            viewModel.setTab(com.example.viewmodel.IrisNavigationTab.ORCHESTRATION)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IrisAlertRed.copy(alpha = 0.2f), contentColor = IrisAlertRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = "Test", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trigger Critic Interceptor Dry Run", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: Swarm Internal Event Bus Logs
        SettingSection(title = "INTERNAL EVENT BUS LOGS (${recentLogs.size})") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, IrisBorder, RoundedCornerShape(10.dp)),
                color = IrisSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    recentLogs.take(5).forEach { log ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "[${log.agentType}]",
                                color = IrisCyanPrimary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = log.message,
                                color = IrisTextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            if (log.latencyMs > 0) {
                                Text(
                                    text = "${log.latencyMs}ms",
                                    color = IrisTextMuted,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom Key Dialog
    if (showKeyDialog) {
        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            title = { Text("Custom Gemini API Key", color = IrisCyanPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter your key if you wish to override the default environment configuration:",
                        color = IrisTextSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IrisCyanPrimary,
                            unfocusedBorderColor = IrisBorder,
                            focusedTextColor = IrisTextPrimary,
                            unfocusedTextColor = IrisTextPrimary
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setCustomApiKey(keyInput.trim())
                        showKeyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IrisCyanPrimary, contentColor = IrisBackground)
                ) {
                    Text("Save Key", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showKeyDialog = false }) {
                    Text("Cancel", color = IrisTextSecondary)
                }
            },
            containerColor = IrisSurfaceElevated
        )
    }
}

@Composable
fun SettingSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = IrisTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        content()
    }
}

@Composable
fun LanguageOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, if (isSelected) IrisCyanPrimary else IrisBorder, RoundedCornerShape(10.dp)),
        color = if (isSelected) IrisCyanPrimary.copy(alpha = 0.15f) else IrisSurfaceElevated,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                color = if (isSelected) IrisCyanPrimary else IrisTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = IrisTextSecondary,
                fontSize = 10.sp
            )
        }
    }
}
