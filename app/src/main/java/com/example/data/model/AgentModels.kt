package com.example.data.model

enum class AgentType(val displayName: String, val roleDescription: String) {
    ORCHESTRATOR("Orchestrator", "Central router & latency budget controller"),
    PLANNER("PlannerAgent", "Decomposes goals into execution DAGs"),
    EXECUTOR("ExecutorAgent", "Invokes native bridges, APIs & AnyClaw CLI"),
    MEMORY("MemoryAgent", "Context, vector search & Knowledge Graphs"),
    CRITIC("CriticAgent", "Safety interceptor & policy validator"),
    PROACTIVE("ProactiveAgent", "Monitors device telemetry & context"),
    EMOTION("EmotionAgent", "Analyzes sentiment & adjusts executive tone")
}

enum class AgentStatus {
    IDLE,
    ANALYZING,
    PLANNING,
    EXECUTING,
    INTERCEPTING,
    SUCCESS,
    WARNING
}

data class SwarmAgentState(
    val type: AgentType,
    val status: AgentStatus = AgentStatus.IDLE,
    val lastActiveMillis: Long = System.currentTimeMillis(),
    val activeTask: String? = null
)

enum class MessageSender {
    USER,
    AGENT,
    SYSTEM_CRITIC,
    SWARM_BUS
}

data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val agentType: AgentType? = null,
    val functionCalls: List<FunctionCallTool> = emptyList(),
    val executionDag: ExecutionDag? = null,
    val criticWarning: CriticWarning? = null,
    val latencyMs: Long? = null,
    val emotionTone: String? = null
)

data class ExecutionDag(
    val goal: String,
    val steps: List<DagStep>
)

data class DagStep(
    val id: String,
    val title: String,
    val toolName: String,
    val parametersJson: String,
    val status: StepStatus = StepStatus.PENDING,
    val output: String? = null
)

enum class StepStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    INTERCEPTED_BY_CRITIC,
    FAILED
}

data class FunctionCallTool(
    val name: String,
    val argumentsJson: String,
    val status: String = "READY"
)

data class CriticWarning(
    val actionName: String,
    val riskLevel: RiskLevel,
    val reason: String,
    val commandPayload: String,
    val isConfirmedByUser: Boolean = false
)

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class TelemetryData(
    val batteryPercent: Int = 100,
    val isCharging: Boolean = false,
    val networkType: String = "WiFi",
    val audioVolumePercent: Int = 75,
    val ambientContext: String = "Executive Desk",
    val latencyBudgetMs: Long = 850
)

enum class AppLanguage(val code: String, val label: String) {
    ENGLISH("en", "English (Executive)"),
    BENGALI("bn", "বাংলা (নির্বাহী)")
}
