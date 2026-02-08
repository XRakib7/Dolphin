package com.softcraft.dolphin.utils

import com.softcraft.dolphin.data.model.AiConfig
import com.softcraft.dolphin.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiHelper @Inject constructor() {

    private val client = OkHttpClient()
    private val mediaType = "application/json".toMediaType()

    suspend fun generateResponse(
        message: String,
        config: AiConfig,
        conversationHistory: List<ChatMessage> = emptyList()
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                // Validate
                if (config.apiKey.isBlank()) throw IllegalArgumentException("API key not configured")
                if (!config.isActive) throw IllegalStateException("AI service inactive")

                // Build request body with conversation history
                val requestBody = buildRequestBody(message, config, conversationHistory)

                // Debug log
                println("Gemini Request URL: https://generativelanguage.googleapis.com/v1beta/models/${config.modelName}:generateContent?key=${config.apiKey.take(10)}...")
                println("Gemini Request Body: $requestBody")

                // Create request
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/${config.modelName}:generateContent?key=${config.apiKey}")
                    .post(requestBody.toRequestBody(mediaType))
                    .addHeader("Content-Type", "application/json")
                    .build()

                // Execute request
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                // Debug log
                println("Gemini Response Code: ${response.code}")
                println("Gemini Response Body: ${responseBody?.take(200)}...")

                if (response.isSuccessful && responseBody != null) {
                    parseResponse(responseBody)
                } else {
                    throw Exception("API request failed: ${response.code} - ${response.message}")
                }
            } catch (e: Exception) {
                throw Exception("Failed to generate response: ${e.message}")
            }
        }
    }

    private fun buildRequestBody(
        message: String,
        config: AiConfig,
        history: List<ChatMessage>
    ): String {
        // Create contents array as per your working format
        val contents = JSONArray()

        // If there's a system prompt, add it as the first message
        if (config.systemPrompt.isNotBlank()) {
            val systemMessage = JSONObject().apply {
                put("role", "user") // System instruction is typically sent as user role
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", config.systemPrompt)
                    })
                })
            }
            contents.put(systemMessage)

            // Add a model response acknowledging the system prompt
            val modelAck = JSONObject().apply {
                put("role", "model")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", "Understood. I'll follow your instructions.")
                    })
                })
            }
            contents.put(modelAck)
        }

        // Add conversation history (alternating user and model messages)
        history.forEach { chatMessage ->
            val role = if (chatMessage.isUser) "user" else "model"
            val messageObj = JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", chatMessage.content)
                    })
                })
            }
            contents.put(messageObj)
        }

        // Add the current user message
        val currentMessage = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply {
                    put("text", message)
                })
            })
        }
        contents.put(currentMessage)

        // Build the final request JSON (simpler format like your working version)
        return JSONObject().apply {
            put("contents", contents)

            // Add safety settings (optional but recommended)
            put("safetySettings", JSONArray().apply {
                put(JSONObject().apply {
                    put("category", "HARM_CATEGORY_HARASSMENT")
                    put("threshold", "BLOCK_MEDIUM_AND_ABOVE")
                })
                put(JSONObject().apply {
                    put("category", "HARM_CATEGORY_HATE_SPEECH")
                    put("threshold", "BLOCK_MEDIUM_AND_ABOVE")
                })
                put(JSONObject().apply {
                    put("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT")
                    put("threshold", "BLOCK_MEDIUM_AND_ABOVE")
                })
                put(JSONObject().apply {
                    put("category", "HARM_CATEGORY_DANGEROUS_CONTENT")
                    put("threshold", "BLOCK_MEDIUM_AND_ABOVE")
                })
            })

            // Add generation config
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7) // Default value
                put("maxOutputTokens", 2048)
            })
        }.toString()
    }

    private fun parseResponse(responseBody: String): String {
        return try {
            val jsonResponse = JSONObject(responseBody)
            jsonResponse
                .optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
                ?: "No response generated"
        } catch (e: Exception) {
            "Failed to parse response: ${e.message}"
        }
    }

    // Alternative simple method (closer to your original working version) we using buildRequestBody
    fun buildSimpleRequestBody(
        message: String,
        config: AiConfig,
        history: List<ChatMessage> = emptyList()
    ): String {
        // Build conversation context from history
        val conversationContext = StringBuilder()

        if (config.systemPrompt.isNotBlank()) {
            conversationContext.append("System: ${config.systemPrompt}\n\n")
        }

        // Add history (last 10 messages to avoid token limits)
        history.takeLast(10).forEach { chatMessage ->
            val role = if (chatMessage.isUser) "User" else "Assistant"
            conversationContext.append("$role: ${chatMessage.content}\n")
        }

        // Add current message
        conversationContext.append("User: $message\n\nAssistant:")

        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", conversationContext.toString())
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 2048)
            })
            put("safetySettings", JSONArray().apply {
                put(JSONObject().apply {
                    put("category", "HARM_CATEGORY_HARASSMENT")
                    put("threshold", "BLOCK_MEDIUM_AND_ABOVE")
                })
                put(JSONObject().apply {
                    put("category", "HARM_CATEGORY_HATE_SPEECH")
                    put("threshold", "BLOCK_MEDIUM_AND_ABOVE")
                })
            })
        }.toString()
    }
}