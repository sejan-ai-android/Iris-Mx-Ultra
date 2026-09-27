package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "iris_memories")
data class MemoryEntity(
    @PrimaryKey val key: String,
    val value: String,
    val category: String = "EXECUTIVE",
    val vectorEmbeddingJson: String = "[0.12, 0.45, 0.78, 0.23, 0.91]",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "knowledge_nodes")
data class KnowledgeNode(
    @PrimaryKey val id: String,
    val label: String,
    val nodeType: String, // e.g. "USER_PREFERENCE", "PROJECT", "DEVICE", "CONTACT"
    val metadataJson: String,
    val linkedNodeIdsJson: String = "[]",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "automations")
data class AutomationRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val triggerType: String, // "TIME", "LOCATION", "BATTERY", "APP_LAUNCH", "VOICE"
    val actionDescription: String,
    val isEnabled: Boolean = true,
    val lastTriggeredAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "agent_event_logs")
data class AgentEventLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val agentType: String,
    val eventType: String,
    val message: String,
    val latencyMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)
