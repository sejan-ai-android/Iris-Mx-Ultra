package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.getAgentColor
import com.example.ui.components.getAgentIcon
import com.example.ui.theme.*
import com.example.viewmodel.IrisNavigationTab
import com.example.viewmodel.IrisViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SwarmStatusMonitorScreen(
    viewModel: IrisViewModel,
    modifier: Modifier = Modifier
) {
    val swarmStates by viewModel.swarmStates.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    var selectedAgent by remember { mutableStateOf<AgentType?>(null) }
    var filterActiveOnly by remember { mutableStateOf(false) }

    val activeCount = remember(swarmStates) {
        swarmStates.values.count { it.status != AgentStatus.IDLE }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IrisBackground)
    ) {
        // Friendly Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "SWARM STATUS MONITOR",
                        color = IrisCyanPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    LiveTelemetryPill(activeCount = activeCount)
                }
                Text(
                    text = "7 Autonomous Agents working seamlessly together",
                    color = IrisTextMuted,
                    fontSize = 11.sp
                )
            }

            // Quick Health Check Button
            Button(
                onClick = { viewModel.triggerSwarmDiagnosticPulse() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = IrisSurfaceElevated,
                    contentColor = IrisCyanPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = Brush.horizontalGradient(listOf(IrisCyanPrimary, IrisVioletSecondary))
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("diagnostic_pulse_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Diagnostic",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Health Check", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Global Health & Status Summary Strip
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            color = IrisSurfaceVariant,
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.horizontalGradient(listOf(IrisBorder, IrisCyanPrimary.copy(alpha = 0.3f)))
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricItem(
                    label = "AGENTS TEAM",
                    value = "7 Active",
                    color = IrisCyanPrimary
                )
                MetricDivider()
                MetricItem(
                    label = "STATUS",
                    value = if (activeCount > 0) "$activeCount Working" else "Ready & Standing by",
                    color = if (activeCount > 0) IrisHazardAmber else IrisEmeraldAccent
                )
                MetricDivider()
                MetricItem(
                    label = "RESPONSE TIME",
                    value = "< 1s Instant",
                    color = IrisVioletSecondary
                )
                MetricDivider()
                MetricItem(
                    label = "GUARDIAN SHIELD",
                    value = "Protected",
                    color = IrisEmeraldAccent
                )
            }
        }

        // Swarm Mesh Network Graph
        SwarmMeshNetworkGraphic(
            states = swarmStates,
            selectedAgent = selectedAgent,
            onSelectAgent = { selectedAgent = if (selectedAgent == it) null else it },
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Filter Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "YOUR ASSISTANT TEAM",
                color = IrisTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = !filterActiveOnly,
                    onClick = { filterActiveOnly = false },
                    label = { Text("All 7", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IrisCyanPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = IrisCyanPrimary
                    )
                )
                FilterChip(
                    selected = filterActiveOnly,
                    onClick = { filterActiveOnly = true },
                    label = { Text("Working ($activeCount)", fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = IrisHazardAmber.copy(alpha = 0.2f),
                        selectedLabelColor = IrisHazardAmber
                    )
                )
            }
        }

        // Detailed Agent Cards
        val displayedAgents = remember(filterActiveOnly, swarmStates) {
            if (filterActiveOnly) {
                AgentType.entries.filter { (swarmStates[it]?.status ?: AgentStatus.IDLE) != AgentStatus.IDLE }
            } else {
                AgentType.entries.toList()
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (displayedAgents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "সকল এজেন্ট প্রস্তুত রয়েছে। আপনার নির্দেশনার অপেক্ষায় রয়েছে।",
                            color = IrisTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            items(displayedAgents, key = { it.name }) { agentType ->
                val state = swarmStates[agentType] ?: SwarmAgentState(agentType)
                val isExpanded = selectedAgent == agentType

                AgentStatusCard(
                    agentType = agentType,
                    state = state,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        selectedAgent = if (selectedAgent == agentType) null else agentType
                    },
                    onAskHelp = {
                        val friendlyPrompt = when (agentType) {
                            AgentType.ORCHESTRATOR -> "হ্যালো আইরিস, আজকের দিনটি কেমন হতে পারে?"
                            AgentType.PLANNER -> "আমার জন্য আজকের কাজের একটি সুন্দর তালিকা তৈরি করো"
                            AgentType.EXECUTOR -> "আমার জন্য সুন্দর একটি গান চালিয়ে দাও"
                            AgentType.MEMORY -> "আমার জন্য একটি গুরুত্বপূর্ণ নোট মনে রাখো"
                            AgentType.CRITIC -> "আমার নিরাপত্তা ও গোপনীয়তা সেটিংস কেমন আছে?"
                            AgentType.PROACTIVE -> "আমার ডিভাইসের ব্যাটারি ও চার্জিং অবস্থা জানাও"
                            AgentType.EMOTION -> "আমি একটু ক্লান্ত অনুভব করছি, কিছু অনুপ্রেরণামূলক কথা বলো"
                        }
                        viewModel.submitPrompt(friendlyPrompt)
                        viewModel.setTab(IrisNavigationTab.ORCHESTRATION)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun LiveTelemetryPill(activeCount: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulsePill")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PillAlpha"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (activeCount > 0) IrisHazardAmber.copy(alpha = 0.2f) else IrisEmeraldAccent.copy(alpha = 0.2f),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (activeCount > 0) IrisHazardAmber else IrisEmeraldAccent
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        (if (activeCount > 0) IrisHazardAmber else IrisEmeraldAccent).copy(alpha = alpha)
                    )
            )
            Text(
                text = if (activeCount > 0) "$activeCount WORKING" else "STANDBY READY",
                color = if (activeCount > 0) IrisHazardAmber else IrisEmeraldAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = IrisTextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun MetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(IrisBorder)
    )
}

@Composable
fun SwarmMeshNetworkGraphic(
    states: Map<AgentType, SwarmAgentState>,
    selectedAgent: AgentType?,
    onSelectAgent: (AgentType) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MeshTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "MeshPhase"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, IrisBorder, RoundedCornerShape(12.dp)),
        color = IrisSurfaceElevated
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            val center = Offset(width / 2f, height / 2f)

            val radius = (height / 2f) * 0.72f
            val nodePositions = remember(width, height) {
                val map = mutableMapOf<AgentType, Offset>()
                map[AgentType.ORCHESTRATOR] = center
                val outerAgents = AgentType.entries.filter { it != AgentType.ORCHESTRATOR }
                outerAgents.forEachIndexed { index, agent ->
                    val angle = Math.toRadians((index * (360.0 / outerAgents.size) - 90.0))
                    val x = center.x + (radius * 1.8f * cos(angle)).toFloat()
                    val y = center.y + (radius * sin(angle)).toFloat()
                    map[agent] = Offset(x, y)
                }
                map
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val orchPos = nodePositions[AgentType.ORCHESTRATOR] ?: center

                nodePositions.forEach { (agent, pos) ->
                    if (agent != AgentType.ORCHESTRATOR) {
                        val state = states[agent]
                        val isAgentActive = (state?.status ?: AgentStatus.IDLE) != AgentStatus.IDLE
                        val lineColor = if (isAgentActive) getAgentColor(agent) else IrisBorder

                        drawLine(
                            color = lineColor.copy(alpha = if (isAgentActive) 0.8f else 0.35f),
                            start = orchPos,
                            end = pos,
                            strokeWidth = if (isAgentActive) 2.5.dp.toPx() else 1.dp.toPx(),
                            pathEffect = if (!isAgentActive) PathEffect.dashPathEffect(floatArrayOf(8f, 8f), phase) else null
                        )
                    }
                }
            }

            nodePositions.forEach { (agent, pos) ->
                val state = states[agent]
                val isActive = (state?.status ?: AgentStatus.IDLE) != AgentStatus.IDLE
                val isSelected = selectedAgent == agent
                val agentColor = getAgentColor(agent)
                val nodeSize = if (agent == AgentType.ORCHESTRATOR) 34.dp else 28.dp
                val xPosDp = with(density) { pos.x.toDp() } - (nodeSize / 2)
                val yPosDp = with(density) { pos.y.toDp() } - (nodeSize / 2)

                Box(
                    modifier = Modifier
                        .offset(x = xPosDp, y = yPosDp)
                        .size(nodeSize)
                        .clip(CircleShape)
                        .background(if (isActive) agentColor else IrisSurface)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color.White else agentColor,
                            shape = CircleShape
                        )
                        .clickable { onSelectAgent(agent) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getAgentIcon(agent),
                        contentDescription = agent.displayName,
                        tint = if (isActive) IrisBackground else agentColor,
                        modifier = Modifier.size(if (agent == AgentType.ORCHESTRATOR) 18.dp else 14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AgentStatusCard(
    agentType: AgentType,
    state: SwarmAgentState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAskHelp: () -> Unit
) {
    val agentColor = getAgentColor(agentType)
    val isActive = state.status != AgentStatus.IDLE

    // Warm, friendly descriptions
    val friendlyRoleDescription = when (agentType) {
        AgentType.ORCHESTRATOR -> "আপনার প্রধান সহযোগী যিনি সব কাজের সমন্বয় ও দ্রুত সমাধান নিশ্চিত করেন।"
        AgentType.PLANNER -> "আপনার লক্ষ্য বা কাজগুলো সুন্দর ধাপে ধাপে গুছিয়ে সাজিয়ে দেন।"
        AgentType.EXECUTOR -> "আপনার অনুরোধ অনুযায়ী অ্যাপ ওপেন, গান বাজানো ও কাজ সরাসরি সম্পন্ন করেন।"
        AgentType.MEMORY -> "আপনার গুরুত্বপূর্ণ তথ্য ও নোটগুলো সুরক্ষিতভাবে মনে রাখেন।"
        AgentType.CRITIC -> "আপনার নিরাপত্তা ও ব্যক্তিগত গোপনীয়তা সবসময় সুরক্ষিত রাখেন।"
        AgentType.PROACTIVE -> "আপনার ডিভাইসের যত্ন নেন এবং সময়োপযোগী সহায়ক পরামর্শ দেন।"
        AgentType.EMOTION -> "আপনার মনের ভাব বোঝেন এবং আন্তরিক বন্ধুসুলভ আচরণ বজায় রাখেন।"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand)
            .testTag("agent_monitor_card_${agentType.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) IrisSurfaceElevated else IrisSurface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isActive) agentColor else if (isExpanded) IrisCyanPrimary.copy(alpha = 0.5f) else IrisBorder
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isActive) agentColor.copy(alpha = 0.25f) else IrisSurfaceVariant)
                        .border(1.dp, agentColor, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getAgentIcon(agentType),
                        contentDescription = agentType.displayName,
                        tint = agentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = agentType.displayName,
                            color = IrisTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isActive) agentColor.copy(alpha = 0.2f) else IrisSurfaceVariant
                        ) {
                            Text(
                                text = if (isActive) "কাজ করছে" else "প্রস্তুত",
                                color = if (isActive) agentColor else IrisTextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = friendlyRoleDescription,
                        color = IrisTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = if (isExpanded) 3 else 1
                    )
                }

                IconButton(onClick = onToggleExpand, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = IrisTextMuted
                    )
                }
            }

            if (state.activeTask != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp)),
                    color = agentColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            strokeWidth = 2.dp,
                            color = agentColor
                        )
                        Text(
                            text = state.activeTask,
                            color = IrisTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = IrisBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = friendlyRoleDescription,
                        color = IrisTextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onAskHelp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = agentColor.copy(alpha = 0.2f),
                            contentColor = IrisTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, agentColor, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "${agentType.displayName}-এর কাছে সাহায্য চান",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Help",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
