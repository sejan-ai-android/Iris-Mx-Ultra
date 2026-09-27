package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgentStatus
import com.example.data.model.AgentType
import com.example.data.model.SwarmAgentState
import com.example.ui.theme.*

@Composable
fun AgentSwarmBar(
    states: Map<AgentType, SwarmAgentState>,
    modifier: Modifier = Modifier
) {
    var selectedAgentForInspection by remember { mutableStateOf<AgentType?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NEURAL SWARM MATRIX (7 AGENTS)",
                color = IrisTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "EVENT BUS: ONLINE",
                color = IrisEmeraldAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AgentType.entries.forEach { agent ->
                val state = states[agent] ?: SwarmAgentState(agent)
                AgentChip(
                    state = state,
                    onClick = {
                        selectedAgentForInspection = if (selectedAgentForInspection == agent) null else agent
                    }
                )
            }
        }

        // Expanded Agent Inspector details
        AnimatedVisibility(visible = selectedAgentForInspection != null) {
            val agent = selectedAgentForInspection
            if (agent != null) {
                val state = states[agent] ?: SwarmAgentState(agent)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("agent_inspection_card"),
                    colors = CardDefaults.cardColors(containerColor = IrisSurfaceElevated),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(getAgentColor(agent))
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = getAgentIcon(agent),
                                contentDescription = agent.displayName,
                                tint = getAgentColor(agent),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = agent.displayName,
                                color = IrisTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Badge(
                                containerColor = if (state.status == AgentStatus.IDLE) IrisBorder else getAgentColor(agent).copy(alpha = 0.2f),
                                contentColor = if (state.status == AgentStatus.IDLE) IrisTextSecondary else getAgentColor(agent)
                            ) {
                                Text(state.status.name, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = agent.roleDescription,
                            color = IrisTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        if (state.activeTask != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Current Task: ${state.activeTask}",
                                color = IrisCyanPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgentChip(
    state: SwarmAgentState,
    onClick: () -> Unit
) {
    val agentColor = getAgentColor(state.type)
    val isActive = state.status != AgentStatus.IDLE

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isActive) agentColor else IrisBorder,
        label = "AgentChipBorder"
    )

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, animatedBorderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("agent_chip_${state.type.name}"),
        color = if (isActive) agentColor.copy(alpha = 0.15f) else IrisSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Status pulse dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isActive) agentColor else IrisTextMuted)
            )

            Icon(
                imageVector = getAgentIcon(state.type),
                contentDescription = state.type.displayName,
                tint = if (isActive) agentColor else IrisTextSecondary,
                modifier = Modifier.size(14.dp)
            )

            Text(
                text = state.type.displayName.replace("Agent", ""),
                color = if (isActive) IrisTextPrimary else IrisTextSecondary,
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

fun getAgentColor(type: AgentType): Color {
    return when (type) {
        AgentType.ORCHESTRATOR -> AgentOrchestratorColor
        AgentType.PLANNER -> AgentPlannerColor
        AgentType.EXECUTOR -> AgentExecutorColor
        AgentType.MEMORY -> AgentMemoryColor
        AgentType.CRITIC -> AgentCriticColor
        AgentType.PROACTIVE -> AgentProactiveColor
        AgentType.EMOTION -> AgentEmotionColor
    }
}

fun getAgentIcon(type: AgentType): ImageVector {
    return when (type) {
        AgentType.ORCHESTRATOR -> Icons.Default.Hub
        AgentType.PLANNER -> Icons.Default.AccountTree
        AgentType.EXECUTOR -> Icons.Default.PlayArrow
        AgentType.MEMORY -> Icons.Default.Memory
        AgentType.CRITIC -> Icons.Default.Security
        AgentType.PROACTIVE -> Icons.Default.Sensors
        AgentType.EMOTION -> Icons.Default.Psychology
    }
}
