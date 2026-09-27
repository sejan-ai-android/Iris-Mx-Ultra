package com.example.data.local

import androidx.room.*
import com.example.data.model.AgentEventLog
import com.example.data.model.AutomationRule
import com.example.data.model.KnowledgeNode
import com.example.data.model.MemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM iris_memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM iris_memories WHERE `key` = :key LIMIT 1")
    suspend fun getMemory(key: String): MemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Query("DELETE FROM iris_memories WHERE `key` = :key")
    suspend fun deleteMemory(key: String)

    @Query("SELECT * FROM iris_memories WHERE `key` LIKE '%' || :query || '%' OR `value` LIKE '%' || :query || '%'")
    suspend fun searchMemories(query: String): List<MemoryEntity>
}

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_nodes ORDER BY updatedAt DESC")
    fun getAllNodes(): Flow<List<KnowledgeNode>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: KnowledgeNode)

    @Query("DELETE FROM knowledge_nodes WHERE id = :id")
    suspend fun deleteNode(id: String)
}

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automations ORDER BY createdAt DESC")
    fun getAllAutomations(): Flow<List<AutomationRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomation(automation: AutomationRule): Long

    @Update
    suspend fun updateAutomation(automation: AutomationRule)

    @Query("DELETE FROM automations WHERE id = :id")
    suspend fun deleteAutomation(id: Long)
}

@Dao
interface AgentEventLogDao {
    @Query("SELECT * FROM agent_event_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<AgentEventLog>>

    @Insert
    suspend fun insertLog(log: AgentEventLog)

    @Query("DELETE FROM agent_event_logs")
    suspend fun clearLogs()
}
