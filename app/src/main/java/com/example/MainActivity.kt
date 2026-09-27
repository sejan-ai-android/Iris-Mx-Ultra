package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.CriticInterceptorDialog
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.IrisNavigationTab
import com.example.viewmodel.IrisViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val irisViewModel: IrisViewModel = viewModel()
                IrisMainApp(viewModel = irisViewModel)
            }
        }
    }
}

@Composable
fun IrisMainApp(viewModel: IrisViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val pendingCriticWarning by viewModel.pendingCriticWarning.collectAsState()

    // Handle system back navigation to return to Orchestration screen
    BackHandler(enabled = currentTab != IrisNavigationTab.ORCHESTRATION) {
        viewModel.setTab(IrisNavigationTab.ORCHESTRATION)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = IrisBackground,
        bottomBar = {
            NavigationBar(
                containerColor = IrisSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("iris_bottom_navigation")
            ) {
                NavigationItemData.entries.forEach { item ->
                    val isSelected = currentTab == item.tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.setTab(item.tab) },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = IrisCyanPrimary,
                            selectedTextColor = IrisCyanPrimary,
                            unselectedIconColor = IrisTextMuted,
                            unselectedTextColor = IrisTextMuted,
                            indicatorColor = IrisSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(IrisBackground)
        ) {
            when (currentTab) {
                IrisNavigationTab.ORCHESTRATION -> OrchestrationScreen(viewModel = viewModel)
                IrisNavigationTab.CODE_GEN -> CodeGenScreen(viewModel = viewModel)
                IrisNavigationTab.MEMORY_GRAPH -> MemoryGraphScreen(viewModel = viewModel)
                IrisNavigationTab.TELEMETRY_AUTOMATIONS -> TelemetryAutomationsScreen(viewModel = viewModel)
                IrisNavigationTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }

            // Critic Safety Interception Dialog
            pendingCriticWarning?.let { warning ->
                CriticInterceptorDialog(
                    warning = warning,
                    onConfirm = { viewModel.confirmCriticAction() },
                    onDismiss = { viewModel.dismissCriticAction() }
                )
            }
        }
    }
}

enum class NavigationItemData(
    val tab: IrisNavigationTab,
    val label: String,
    val icon: ImageVector
) {
    ORCHESTRATION(IrisNavigationTab.ORCHESTRATION, "Swarm HUD", Icons.Default.Hub),
    CODE_GEN(IrisNavigationTab.CODE_GEN, "Code Engine", Icons.Default.Code),
    MEMORY_GRAPH(IrisNavigationTab.MEMORY_GRAPH, "Memory Graph", Icons.Default.AccountTree),
    TELEMETRY(IrisNavigationTab.TELEMETRY_AUTOMATIONS, "Telemetry", Icons.Default.Sensors),
    SETTINGS(IrisNavigationTab.SETTINGS, "Core Config", Icons.Default.Settings)
}
