package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.remote.GeminiApiClient
import com.example.data.repository.IrisAgentRepository
import com.example.service.AnyClawBridge
import com.example.service.NativeDeviceBridge
import com.example.service.SpeechManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class IrisNavigationTab {
    ORCHESTRATION,
    CODE_GEN,
    MEMORY_GRAPH,
    TELEMETRY_AUTOMATIONS,
    SETTINGS
}

class IrisViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val nativeBridge = NativeDeviceBridge(application)
    private val anyClawBridge = AnyClawBridge()
    private val geminiApiClient = GeminiApiClient()

    val repository = IrisAgentRepository(
        database = database,
        nativeBridge = nativeBridge,
        anyClawBridge = anyClawBridge,
        geminiApiClient = geminiApiClient
    )

    val speechManager = SpeechManager(application) { voiceQuery ->
        submitPrompt(voiceQuery)
    }

    // UI States
    private val _currentTab = MutableStateFlow(IrisNavigationTab.ORCHESTRATION)
    val currentTab: StateFlow<IrisNavigationTab> = _currentTab.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _currentActiveDag = MutableStateFlow<ExecutionDag?>(null)
    val currentActiveDag: StateFlow<ExecutionDag?> = _currentActiveDag.asStateFlow()

    private val _telemetry = MutableStateFlow(nativeBridge.readTelemetry())
    val telemetry: StateFlow<TelemetryData> = _telemetry.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _autoSpeakResponse = MutableStateFlow(true)
    val autoSpeakResponse: StateFlow<Boolean> = _autoSpeakResponse.asStateFlow()

    val swarmStates = repository.swarmStates
    val pendingCriticWarning = repository.pendingCriticWarning
    val memories = repository.memoriesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val knowledgeNodes = repository.knowledgeNodesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val automations = repository.automationsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentLogs = repository.recentLogsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isListening = speechManager.isListening
    val isSpeaking = speechManager.isSpeaking
    val audioRmsDb = speechManager.audioRmsDb

    init {
        // Initial welcome message
        _messages.value = listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.AGENT,
                text = "IRIS-MX Ultra Neural Execution Core v1.0.0 active.\n7 Swarm Agents synchronized. Ready for voice or directive input.",
                agentType = AgentType.ORCHESTRATOR,
                latencyMs = 18,
                emotionTone = "Executive Chief of Staff"
            )
        )

        // Periodic telemetry polling
        viewModelScope.launch {
            while (true) {
                _telemetry.value = nativeBridge.readTelemetry()
                delay(3000)
            }
        }
    }

    fun setTab(tab: IrisNavigationTab) {
        _currentTab.value = tab
        nativeBridge.triggerHapticFeedback(false)
    }

    fun setInputText(text: String) {
        _inputText.value = text
    }

    fun setLanguage(language: AppLanguage) {
        _appLanguage.value = language
        speechManager.setLanguage(language)
        nativeBridge.triggerHapticFeedback(false)
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
    }

    fun toggleAutoSpeak() {
        _autoSpeakResponse.value = !_autoSpeakResponse.value
    }

    fun toggleListening() {
        if (speechManager.isListening.value) {
            speechManager.stopListening()
            nativeBridge.triggerHapticFeedback(false)
        } else {
            nativeBridge.triggerHapticFeedback(true)
            speechManager.startListening()
        }
    }

    fun submitPrompt(promptText: String = _inputText.value) {
        val trimmed = promptText.trim()
        if (trimmed.isBlank() || _isProcessing.value) return

        nativeBridge.triggerHapticFeedback(false)
        _inputText.value = ""
        _isProcessing.value = true

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.USER,
            text = trimmed
        )
        _messages.value = _messages.value + userMessage

        viewModelScope.launch {
            try {
                val agentResponse = repository.processUserPrompt(
                    prompt = trimmed,
                    language = _appLanguage.value,
                    apiKeyOverride = _customApiKey.value.takeIf { it.isNotBlank() },
                    onStepUpdated = { dag ->
                        _currentActiveDag.value = dag
                    }
                )
                _messages.value = _messages.value + agentResponse

                if (_autoSpeakResponse.value) {
                    speechManager.speak(agentResponse.text)
                }
            } catch (e: Exception) {
                val errorMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sender = MessageSender.AGENT,
                    text = "System fault: ${e.message}",
                    agentType = AgentType.ORCHESTRATOR
                )
                _messages.value = _messages.value + errorMsg
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun confirmCriticAction() {
        nativeBridge.triggerHapticFeedback(true)
        repository.confirmCriticWarning {
            // Update messages with approval notice
            val msg = ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.AGENT,
                text = "CRITIC AUTHORIZED: High-risk action successfully executed by Commander override.",
                agentType = AgentType.CRITIC
            )
            _messages.value = _messages.value + msg
        }
    }

    fun dismissCriticAction() {
        nativeBridge.triggerHapticFeedback(false)
        repository.dismissCriticWarning()
    }

    fun saveMemory(key: String, value: String, category: String = "USER") {
        viewModelScope.launch {
            repository.saveMemory(key, value, category)
        }
    }

    fun deleteMemory(key: String) {
        viewModelScope.launch {
            repository.deleteMemory(key)
        }
    }

    fun createAutomation(title: String, triggerType: String, actionDesc: String) {
        viewModelScope.launch {
            repository.addAutomation(title, triggerType, actionDesc)
        }
    }

    fun deleteAutomation(id: Long) {
        viewModelScope.launch {
            repository.deleteAutomation(id)
        }
    }

    fun stopSpeaking() {
        speechManager.stopSpeaking()
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
    }
}
