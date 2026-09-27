package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.IrisViewModel

@Composable
fun TelemetryAutomationsScreen(
    viewModel: IrisViewModel,
    modifier: Modifier = Modifier
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val automations by viewModel.automations.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newTriggerType by remember { mutableStateOf("BATTERY") }
    var newActionDesc by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IrisBackground)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "TELEMETRY & AUTOMATIONS",
                    color = IrisCyanPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Hardware Telemetry & Proactive Workflows",
                    color = IrisTextMuted,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(IrisCyanPrimary)
                    .testTag("add_automation_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Automation",
                    tint = IrisBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hardware Telemetry Matrix Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Battery Card
            TelemetryCard(
                icon = if (telemetry.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                iconColor = if (telemetry.batteryPercent > 20) IrisEmeraldAccent else IrisAlertRed,
                title = "BATTERY",
                value = "${telemetry.batteryPercent}%",
                subtitle = if (telemetry.isCharging) "Charging (AC Dock)" else "Discharging (Grid)",
                modifier = Modifier.weight(1f)
            )

            // Audio Volume Card
            TelemetryCard(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                iconColor = IrisCyanPrimary,
                title = "AUDIO SESSION",
                value = "${telemetry.audioVolumePercent}%",
                subtitle = "Stream: Music/Media",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Network Card
            TelemetryCard(
                icon = Icons.Default.Wifi,
                iconColor = IrisVioletSecondary,
                title = "NETWORK LINK",
                value = telemetry.networkType,
                subtitle = "Sub-10ms Routing",
                modifier = Modifier.weight(1f)
            )

            // Latency Budget Card
            TelemetryCard(
                icon = Icons.Default.Speed,
                iconColor = IrisHazardAmber,
                title = "LATENCY BUDGET",
                value = "${telemetry.latencyBudgetMs}ms",
                subtitle = telemetry.ambientContext,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ProactiveAgent Suggestion Banner
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, IrisProactiveColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            color = IrisProactiveColor.copy(alpha = 0.12f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Proactive Insight",
                    tint = IrisProactiveColor,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PROACTIVE AGENT INSIGHT",
                        color = IrisProactiveColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (telemetry.batteryPercent > 20) {
                            "Device telemetry nominal (${telemetry.batteryPercent}%). Sub-second Swarm execution prioritized."
                        } else {
                            "Battery critically low (${telemetry.batteryPercent}%). ProactiveAgent recommends switching to local edge cache."
                        },
                        color = IrisTextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Automations List
        Text(
            text = "SCHEDULED & EVENT AUTOMATIONS (${automations.size})",
            color = IrisTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(automations, key = { it.id }) { rule ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, IrisBorder, RoundedCornerShape(10.dp)),
                    color = IrisSurfaceElevated
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(IrisCyanPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (rule.triggerType) {
                                    "BATTERY" -> Icons.Default.BatteryAlert
                                    "TIME" -> Icons.Default.Schedule
                                    "LOCATION" -> Icons.Default.Place
                                    "VOICE" -> Icons.Default.RecordVoiceOver
                                    else -> Icons.Default.PlayCircle
                                },
                                contentDescription = rule.triggerType,
                                tint = IrisCyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = rule.title,
                                    color = IrisTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Badge(
                                    containerColor = IrisSurfaceVariant,
                                    contentColor = IrisEmeraldAccent
                                ) {
                                    Text(rule.triggerType, fontSize = 9.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = rule.actionDescription,
                                color = IrisTextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }

                        IconButton(
                            onClick = { viewModel.deleteAutomation(rule.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = IrisAlertRed.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Automation Dialog
    if (showAddDialog) {
        val triggerTypes = listOf("BATTERY", "TIME", "LOCATION", "APP_LAUNCH", "VOICE")
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Create Automated Workflow", color = IrisCyanPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Rule Title") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IrisCyanPrimary,
                            unfocusedBorderColor = IrisBorder,
                            focusedTextColor = IrisTextPrimary,
                            unfocusedTextColor = IrisTextPrimary
                        )
                    )

                    Text("Trigger Type:", color = IrisTextSecondary, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        triggerTypes.take(3).forEach { type ->
                            FilterChip(
                                selected = newTriggerType == type,
                                onClick = { newTriggerType = type },
                                label = { Text(type, fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newActionDesc,
                        onValueChange = { newActionDesc = it },
                        label = { Text("Action Description") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IrisCyanPrimary,
                            unfocusedBorderColor = IrisBorder,
                            focusedTextColor = IrisTextPrimary,
                            unfocusedTextColor = IrisTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank() && newActionDesc.isNotBlank()) {
                            viewModel.createAutomation(newTitle.trim(), newTriggerType, newActionDesc.trim())
                            newTitle = ""
                            newActionDesc = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IrisCyanPrimary, contentColor = IrisBackground)
                ) {
                    Text("Register Rule", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = IrisTextSecondary)
                }
            },
            containerColor = IrisSurfaceElevated
        )
    }
}

@Composable
fun TelemetryCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, IrisBorder, RoundedCornerShape(12.dp)),
        color = IrisSurfaceElevated
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    color = IrisTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = IrisTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
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
