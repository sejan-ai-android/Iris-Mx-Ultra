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
    SWARM_MONITOR,
    ORCHESTRATION,
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

    private val _appLanguage = MutableStateFlow(AppLanguage.BENGALI)
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
    val handsFreeContinuous = speechManager.handsFreeContinuous
    val voicePreset = speechManager.voicePreset

    // AI Model Engine: Gemini 3.1 Flash Live (Default)
    private val _audioEngineMode = MutableStateFlow(AudioEngineMode.DEFAULT)
    val audioEngineMode: StateFlow<AudioEngineMode> = _audioEngineMode.asStateFlow()

    // Personality Mode: Assistant Mode (Default)
    private val _personalityMode = MutableStateFlow(PersonalityMode.DEFAULT)
    val personalityMode: StateFlow<PersonalityMode> = _personalityMode.asStateFlow()

    // Dynamic Addressing: Boss (Default)
    private val _addressingMode = MutableStateFlow(AddressingMode.DEFAULT)
    val addressingMode: StateFlow<AddressingMode> = _addressingMode.asStateFlow()

    private val _customUserName = MutableStateFlow("")
    val customUserName: StateFlow<String> = _customUserName.asStateFlow()

    private val _isAccessibilityActive = MutableStateFlow(nativeBridge.isAccessibilityActive())
    val isAccessibilityActive: StateFlow<Boolean> = _isAccessibilityActive.asStateFlow()

    init {
        // Initial warm, human-like welcome message with hands-free readiness
        _messages.value = listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.AGENT,
                text = "হ্যালো বস! আমি IRIS, আপনার সার্বক্ষণিক ব্যক্তিগত সহকারী।\nGemini 3.1 Flash Live ইঞ্জিন ও হ্যান্ডস-ফ্রি মোড প্রস্তুত—যেকোনো কথা বা ডিভাইসের কাজ সরাসরি মুখে বলুন!",
                agentType = AgentType.ORCHESTRATOR,
                emotionTone = "বন্ধুসুলভ ও আন্তরিক"
            )
        )

        // Periodic telemetry & accessibility status polling
        viewModelScope.launch {
            while (true) {
                _telemetry.value = nativeBridge.readTelemetry()
                _isAccessibilityActive.value = nativeBridge.isAccessibilityActive()
                delay(2500)
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

    fun triggerSwarmDiagnosticPulse() {
        viewModelScope.launch {
            repository.runSwarmDiagnosticPulse()
        }
    }

    fun setVoicePreset(preset: GeminiVoicePreset) {
        speechManager.setVoicePreset(preset)
        nativeBridge.triggerHapticFeedback(false)
    }

    fun setAddressingMode(mode: AddressingMode) {
        _addressingMode.value = mode
        nativeBridge.triggerHapticFeedback(false)
    }

    fun setCustomUserName(name: String) {
        _customUserName.value = name
    }

    fun setPersonalityMode(mode: PersonalityMode) {
        _personalityMode.value = mode
        nativeBridge.triggerHapticFeedback(false)
    }

    fun setAudioEngineMode(engine: AudioEngineMode) {
        _audioEngineMode.value = engine
        nativeBridge.triggerHapticFeedback(false)
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

    fun toggleHandsFreeContinuous() {
        val next = !speechManager.handsFreeContinuous.value
        speechManager.setHandsFreeContinuous(next)
        nativeBridge.triggerHapticFeedback(false)
    }

    fun openAccessibilitySettings() {
        nativeBridge.openAccessibilitySettings()
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
                    voicePreset = speechManager.voicePreset.value,
                    addressingMode = _addressingMode.value,
                    customUserName = _customUserName.value,
                    personalityMode = _personalityMode.value,
                    audioEngineMode = _audioEngineMode.value,
                    onStepUpdated = { dag ->
                        _currentActiveDag.value = dag
                    }
                )
                _messages.value = _messages.value + agentResponse

                if (_autoSpeakResponse.value) {
                    speechManager.speak(agentResponse.text)
                }
            } catch (e: Exception) {
                val errorMsgText = if (_appLanguage.value == AppLanguage.BENGALI) {
                    "দুঃখিত, কাজটি করার সময় একটু সমস্যা দেখা দিয়েছে। তবে চিন্তা করবেন না, আপনি চাইলে আমি আবার চেষ্টা করতে পারি!"
                } else {
                    "I ran into a small hiccup while handling that for you. Don't worry, feel free to try again!"
                }
                val errorMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sender = MessageSender.AGENT,
                    text = errorMsgText,
                    agentType = AgentType.ORCHESTRATOR,
                    emotionTone = "সহানুভূতিশীল"
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
            // Update messages with warm, friendly approval notice
            val msgText = if (_appLanguage.value == AppLanguage.BENGALI) {
                "আপনার নির্দেশ অনুযায়ী কাজটি নিরাপদে সম্পন্ন করে দিয়েছি! আর কিছু করতে হবে কি?"
            } else {
                "I've safely taken care of that for you as confirmed! Anything else you need?"
            }
            val msg = ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.AGENT,
                text = msgText,
                agentType = AgentType.CRITIC,
                emotionTone = "সহায়তাকারী ও যত্নশীল"
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
