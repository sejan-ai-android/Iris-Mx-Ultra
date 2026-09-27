package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.AppLanguage
import com.example.data.model.GeminiVoicePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechManager(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioRmsDb = MutableStateFlow(0f)
    val audioRmsDb: StateFlow<Float> = _audioRmsDb.asStateFlow()

    private val _currentLanguage = MutableStateFlow(AppLanguage.BENGALI)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    // Voice Preset: Puck (Default)
    private val _voicePreset = MutableStateFlow(GeminiVoicePreset.DEFAULT)
    val voicePreset: StateFlow<GeminiVoicePreset> = _voicePreset.asStateFlow()

    // Hands-free continuous listening mode enabled by default
    private val _handsFreeContinuous = MutableStateFlow(true)
    val handsFreeContinuous: StateFlow<Boolean> = _handsFreeContinuous.asStateFlow()

    private var shouldKeepListening = true
    private var restartRunnable: Runnable? = null

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                applyLanguageToTts(_currentLanguage.value)
                applyVoicePresetToTts(_voicePreset.value)
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                        mainHandler.post {
                            // Pause listening while TTS is speaking to prevent echo
                            stopListeningInternal()
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        // Automatically resume hands-free listening when TTS completes
                        if (_handsFreeContinuous.value && shouldKeepListening) {
                            scheduleRestartListening(400)
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        if (_handsFreeContinuous.value && shouldKeepListening) {
                            scheduleRestartListening(400)
                        }
                    }
                })
            }
        }
    }

    fun setHandsFreeContinuous(enabled: Boolean) {
        _handsFreeContinuous.value = enabled
        shouldKeepListening = enabled
        if (enabled && !_isListening.value && !_isSpeaking.value) {
            startListening()
        } else if (!enabled) {
            cancelScheduledRestart()
        }
    }

    fun setVoicePreset(preset: GeminiVoicePreset) {
        _voicePreset.value = preset
        applyVoicePresetToTts(preset)
    }

    private fun applyVoicePresetToTts(preset: GeminiVoicePreset) {
        textToSpeech?.setPitch(preset.pitch)
        textToSpeech?.setSpeechRate(preset.speed)
        Log.i("SpeechManager", "Applied Voice Preset: ${preset.voiceName} (Pitch: ${preset.pitch}, Speed: ${preset.speed})")
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        applyLanguageToTts(language)
    }

    private fun applyLanguageToTts(language: AppLanguage) {
        val locale = when (language) {
            AppLanguage.ENGLISH -> Locale.US
            AppLanguage.BENGALI -> Locale.forLanguageTag("bn-BD")
        }
        val result = textToSpeech?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w("SpeechManager", "TTS locale $locale not fully supported, falling back to US")
            textToSpeech?.language = Locale.US
        }
    }

    fun startListening() {
        shouldKeepListening = true
        cancelScheduledRestart()
        mainHandler.post {
            startListeningInternal()
        }
    }

    private fun startListeningInternal() {
        if (_isSpeaking.value) {
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("SpeechManager", "Speech recognition not available on device")
            return
        }

        try {
            cleanupRecognizer()

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        _audioRmsDb.value = rmsdB.coerceIn(0f, 10f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        Log.w("SpeechManager", "Speech recognition status code: $error")

                        // Under hands-free mode, seamlessly restart on silence, timeouts, or client pauses
                        if (_handsFreeContinuous.value && shouldKeepListening && !_isSpeaking.value) {
                            when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH,
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                                SpeechRecognizer.ERROR_CLIENT,
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                                    scheduleRestartListening(300)
                                }
                                else -> {
                                    scheduleRestartListening(700)
                                }
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        if (!text.isNullOrBlank()) {
                            onSpeechResult(text)
                        } else if (_handsFreeContinuous.value && shouldKeepListening && !_isSpeaking.value) {
                            scheduleRestartListening(300)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                val langTag = if (_currentLanguage.value == AppLanguage.BENGALI) "bn-BD" else "en-US"
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error starting listening", e)
            _isListening.value = false
            if (_handsFreeContinuous.value && shouldKeepListening) {
                scheduleRestartListening(1000)
            }
        }
    }

    private fun scheduleRestartListening(delayMs: Long) {
        cancelScheduledRestart()
        restartRunnable = Runnable {
            if (_handsFreeContinuous.value && shouldKeepListening && !_isSpeaking.value) {
                startListeningInternal()
            }
        }
        restartRunnable?.let { mainHandler.postDelayed(it, delayMs) }
    }

    private fun cancelScheduledRestart() {
        restartRunnable?.let { mainHandler.removeCallbacks(it) }
        restartRunnable = null
    }

    private fun stopListeningInternal() {
        try {
            speechRecognizer?.stopListening()
            _isListening.value = false
        } catch (_: Exception) {}
    }

    fun stopListening() {
        shouldKeepListening = false
        cancelScheduledRestart()
        mainHandler.post {
            stopListeningInternal()
        }
    }

    fun speak(text: String) {
        if (!isTtsReady) return
        cancelScheduledRestart()
        mainHandler.post {
            stopListeningInternal()
        }
        textToSpeech?.stop()

        // Clean speech text for instant crisp pronunciation
        val cleanSpeech = text
            .replace(Regex("```[\\s\\S]*?```"), "")
            .replace(Regex("[#*`_{}\\[\\]()~]"), "")
            .trim()

        if (cleanSpeech.isNotBlank()) {
            textToSpeech?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, null, "IRIS_STREAM_${System.currentTimeMillis()}")
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _isSpeaking.value = false
        if (_handsFreeContinuous.value && shouldKeepListening) {
            scheduleRestartListening(200)
        }
    }

    private fun cleanupRecognizer() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}
    }

    fun destroy() {
        cancelScheduledRestart()
        shouldKeepListening = false
        cleanupRecognizer()
        textToSpeech?.shutdown()
    }
}
