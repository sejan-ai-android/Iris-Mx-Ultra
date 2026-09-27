package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.GeminiVoicePreset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveWebSocketClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for streaming
        .pingInterval(15, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "GeminiLiveClient"
        private const val LIVE_WS_URL =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
    }

    private var webSocket: WebSocket? = null
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentStreamingText = MutableStateFlow("")
    val currentStreamingText: StateFlow<String> = _currentStreamingText.asStateFlow()

    private var activeTurnDeferred: CompletableDeferred<String>? = null
    private val accumulatedTurnResponse = StringBuilder()

    private var activeVoicePreset: GeminiVoicePreset = GeminiVoicePreset.DEFAULT
    private var activeSystemPrompt: String = ""

    fun configureSession(voicePreset: GeminiVoicePreset, systemPrompt: String) {
        this.activeVoicePreset = voicePreset
        this.activeSystemPrompt = systemPrompt
    }

    /**
     * Connect or ensure active WebSocket bidirectional streaming session
     */
    fun connect(apiKeyOverride: String? = null) {
        if (_isConnected.value && webSocket != null) return

        val apiKey = apiKeyOverride?.takeIf { it.isNotBlank() }
            ?: (try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" })

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "No valid API key for Gemini Live WebSocket")
            return
        }

        val request = Request.Builder()
            .url("$LIVE_WS_URL?key=$apiKey")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.i(TAG, "Gemini 3.1 Flash Live Bidirectional WebSocket Connected")
                _isConnected.value = true
                sendSetupHandshake(ws)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "Gemini Live WebSocket Closing: $code $reason")
                _isConnected.value = false
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Gemini Live WebSocket failure: ${t.message}")
                _isConnected.value = false
                activeTurnDeferred?.completeExceptionally(t)
                activeTurnDeferred = null
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.i(TAG, "Gemini Live WebSocket Closed: $code")
                _isConnected.value = false
            }
        })
    }

    private fun sendSetupHandshake(ws: WebSocket) {
        try {
            val setupObj = JSONObject()
            val setupPayload = JSONObject()
            setupPayload.put("model", "models/gemini-2.0-flash-exp")

            val genConfig = JSONObject()
            genConfig.put("responseModalities", JSONArray().put("TEXT"))

            val speechConfig = JSONObject()
            val voiceConfig = JSONObject()
            val prebuilt = JSONObject()
            prebuilt.put("voiceName", activeVoicePreset.voiceName)
            voiceConfig.put("prebuiltVoiceConfig", prebuilt)
            speechConfig.put("voiceConfig", voiceConfig)
            genConfig.put("speechConfig", speechConfig)

            setupPayload.put("generationConfig", genConfig)

            if (activeSystemPrompt.isNotBlank()) {
                val sysInst = JSONObject()
                val parts = JSONArray().put(JSONObject().put("text", activeSystemPrompt))
                sysInst.put("parts", parts)
                setupPayload.put("systemInstruction", sysInst)
            }

            setupObj.put("setup", setupPayload)
            ws.send(setupObj.toString())
            Log.i(TAG, "Sent setup handshake with voice preset: ${activeVoicePreset.voiceName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating setup handshake", e)
        }
    }

    private fun handleIncomingMessage(jsonString: String) {
        try {
            val root = JSONObject(jsonString)

            if (root.has("serverContent")) {
                val serverContent = root.getJSONObject("serverContent")
                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            val textChunk = part.optString("text", "")
                            if (textChunk.isNotEmpty()) {
                                accumulatedTurnResponse.append(textChunk)
                                _currentStreamingText.value = accumulatedTurnResponse.toString()
                            }
                        }
                    }
                }

                val turnComplete = serverContent.optBoolean("turnComplete", false)
                if (turnComplete) {
                    val fullResponse = accumulatedTurnResponse.toString().trim()
                    activeTurnDeferred?.complete(fullResponse)
                    activeTurnDeferred = null
                    accumulatedTurnResponse.clear()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini Live WebSocket message", e)
        }
    }

    /**
     * Send user turn over Bidirectional WebSocket and await sub-second streamed completion
     */
    suspend fun streamLiveTurn(
        userPrompt: String,
        apiKeyOverride: String?,
        voicePreset: GeminiVoicePreset,
        systemPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        configureSession(voicePreset, systemPrompt)

        if (!_isConnected.value || webSocket == null) {
            connect(apiKeyOverride)
            // Wait brief moment for connection handshake
            var waited = 0
            while (!_isConnected.value && waited < 1200) {
                delay(80)
                waited += 80
            }
        }

        val ws = webSocket
        if (ws == null || !_isConnected.value) {
            return@withContext Result.failure(IllegalStateException("Gemini Live WebSocket not connected"))
        }

        accumulatedTurnResponse.clear()
        _currentStreamingText.value = ""
        val deferred = CompletableDeferred<String>()
        activeTurnDeferred = deferred

        try {
            val clientContent = JSONObject()
            val turnsArray = JSONArray()
            val userTurn = JSONObject()
            userTurn.put("role", "user")
            val parts = JSONArray().put(JSONObject().put("text", userPrompt))
            userTurn.put("parts", parts)
            turnsArray.put(userTurn)

            clientContent.put("turns", turnsArray)
            clientContent.put("turnComplete", true)

            val turnPayload = JSONObject()
            turnPayload.put("clientContent", clientContent)

            ws.send(turnPayload.toString())

            // Wait with a strict latency budget
            val response = withTimeoutOrNull(4500) {
                deferred.await()
            }

            if (!response.isNullOrBlank()) {
                Result.success(response)
            } else {
                Result.failure(Exception("Gemini Live WebSocket stream timed out"))
            }
        } catch (e: Exception) {
            deferred.completeExceptionally(e)
            Result.failure(e)
        }
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "Normal closure")
            webSocket = null
            _isConnected.value = false
        } catch (_: Exception) {}
    }
}
