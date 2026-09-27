package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AgentEventLog
import com.example.data.model.AutomationRule
import com.example.data.model.KnowledgeNode
import com.example.data.model.MemoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MemoryEntity::class,
        KnowledgeNode::class,
        AutomationRule::class,
        AgentEventLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun automationDao(): AutomationDao
    abstract fun agentEventLogDao(): AgentEventLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "iris_mx_ultra.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                populateInitialData(getDatabase(context))
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            // Seed sample memory records & knowledge graph
            database.memoryDao().insertMemory(
                MemoryEntity(
                    key = "user_operating_posture",
                    value = "Executive Chief of Staff - Sub-second latency, zero fluff, proactive briefings.",
                    category = "SYSTEM",
                    vectorEmbeddingJson = "[0.88, 0.42, 0.91, 0.15, 0.77]"
                )
            )
            database.memoryDao().insertMemory(
                MemoryEntity(
                    key = "preferred_audio_channel",
                    value = "Bose QC Ultra / Spotify VIP stream / Subwoofer profile",
                    category = "DEVICE",
                    vectorEmbeddingJson = "[0.32, 0.78, 0.61, 0.85, 0.44]"
                )
            )
            database.memoryDao().insertMemory(
                MemoryEntity(
                    key = "security_clearance_mode",
                    value = "CriticAgent Level 4 - Intercept destructive commands, require biometrics or manual approval.",
                    category = "SECURITY",
                    vectorEmbeddingJson = "[0.95, 0.21, 0.88, 0.34, 0.99]"
                )
            )

            // Seed Knowledge Graph Nodes
            database.knowledgeDao().insertNode(
                KnowledgeNode(
                    id = "node_user_profile",
                    label = "Commander SFC Delta",
                    nodeType = "USER",
                    metadataJson = "{\"role\":\"Executive Commander\",\"org\":\"IRIS-MX Ultra\",\"priority\":\"HIGH\"}"
                )
            )
            database.knowledgeDao().insertNode(
                KnowledgeNode(
                    id = "node_swarm_engine",
                    label = "7-Agent Neural Swarm",
                    nodeType = "SYSTEM",
                    metadataJson = "{\"agents\":[\"Orchestrator\",\"Planner\",\"Executor\",\"Memory\",\"Critic\",\"Proactive\",\"Emotion\"],\"latencyTargetMs\":850}"
                )
            )
            database.knowledgeDao().insertNode(
                KnowledgeNode(
                    id = "node_terminal_anyclaw",
                    label = "AnyClaw CLI Gateway",
                    nodeType = "BRIDGE",
                    metadataJson = "{\"status\":\"ONLINE\",\"version\":\"2.4.0\",\"mode\":\"RESTRICTED_SANDBOX\"}"
                )
            )

            // Seed initial Automations
            database.automationDao().insertAutomation(
                AutomationRule(
                    title = "Low Battery Protocol",
                    triggerType = "BATTERY",
                    actionDescription = "Switch to low-latency edge cache, dim display brightness, mute non-critical agent chatter."
                )
            )
            database.automationDao().insertAutomation(
                AutomationRule(
                    title = "Executive Morning Briefing",
                    triggerType = "TIME",
                    actionDescription = "Compile calendar DAG, pull priority telemetry, summarize urgent notifications in concise bullet points."
                )
            )

            // Seed initial log
            database.agentEventLogDao().insertLog(
                AgentEventLog(
                    agentType = "Orchestrator",
                    eventType = "SWARM_INITIALIZED",
                    message = "IRIS-MX Ultra Neural Execution Core v1.0.0 is online. 7 Agents operational.",
                    latencyMs = 12
                )
            )
        }
    }
}
