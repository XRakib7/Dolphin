package com.softcraft.dolphin.utils

import com.softcraft.dolphin.data.model.AiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiHelper @Inject constructor() {

    private val client = OkHttpClient()
    private val mediaType = "application/json".toMediaType()

    suspend fun generateResponse(
        message: String,
        config: AiConfig
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                // Validate
                if (config.apiKey.isBlank()) throw IllegalArgumentException("API key not configured")
                if (!config.isActive) throw IllegalStateException("AI service inactive")

                // Build request body
                val requestBody = JSONObject().apply {
                    put("contents", JSONObject().apply {
                        put("parts", JSONObject().apply {
                            put("text", "${config.systemPrompt}\n\nUser: $message\n\nAssistant:")
                        })
                    })
                }.toString()

                // Create request
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/${config.modelName}:generateContent?key=${config.apiKey}")
                    .post(requestBody.toRequestBody(mediaType))
                    .addHeader("Content-Type", "application/json")
                    .build()

                // Execute request
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    jsonResponse
                        .optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                        ?: "No response generated"
                } else {
                    throw Exception("API request failed: ${response.code} - ${response.message}")
                }
            } catch (e: Exception) {
                throw Exception("Failed to generate response: ${e.message}")
            }
        }
    }
}