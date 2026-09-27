package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateSwarmResponse(
        prompt: String,
        systemInstruction: String,
        apiKeyOverride: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyOverride?.takeIf { it.isNotBlank() }
            ?: (try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" })

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY is not configured. Falling back to local offline neural multi-agent planner."))
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            // Construct payload
            val rootJson = JSONObject()

            // System instruction
            val sysInstructionObj = JSONObject()
            val sysPartsArray = JSONArray()
            sysPartsArray.put(JSONObject().put("text", systemInstruction))
            sysInstructionObj.put("parts", sysPartsArray)
            rootJson.put("systemInstruction", sysInstructionObj)

            // Contents
            val contentsArray = JSONArray()
            val userContent = JSONObject()
            val userParts = JSONArray()
            userParts.put(JSONObject().put("text", prompt))
            userContent.put("parts", userParts)
            contentsArray.put(userContent)
            rootJson.put("contents", contentsArray)

            // Generation config - Optimized for ultra-low latency sub-second delivery
            val generationConfig = JSONObject()
            generationConfig.put("temperature", 0.2)
            generationConfig.put("topP", 0.85)
            generationConfig.put("maxOutputTokens", 160)
            rootJson.put("generationConfig", generationConfig)

            val requestBody = rootJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiApiClient", "API error: ${response.code} - $bodyString")
                return@withContext Result.failure(Exception("HTTP ${response.code}: $bodyString"))
            }

            val responseJson = JSONObject(bodyString)
            val candidates = responseJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text", "") ?: ""

            if (text.isNotBlank()) {
                Result.success(text)
            } else {
                Result.failure(Exception("Empty response received from Gemini"))
            }
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Request failure", e)
            Result.failure(e)
        }
    }
}
