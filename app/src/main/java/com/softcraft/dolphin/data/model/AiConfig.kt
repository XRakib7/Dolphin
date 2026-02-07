package com.softcraft.dolphin.data.model

import com.google.firebase.Timestamp

data class AiConfig(
    val apiKey: String = "",
    val modelName: String = "",
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val maxOutputTokens: Int = 2048,
    val systemPrompt: String = "You are a helpful assistant.",
    val isActive: Boolean = true,
    val version: String = "1.0",
    val updatedAt: Timestamp? = null,
    val availableModels: Map<String, String> = emptyMap()
) {
    companion object {
        fun fromMap(map: Map<String, Any>): AiConfig {
            return AiConfig(
                apiKey = map["api_key"] as? String ?: "",
                modelName = map["model_name"] as? String ?: "gemini-pro",
                temperature = (map["temperature"] as? Double)?.toFloat() ?: 0.7f,
                topK = (map["top_k"] as? Long)?.toInt() ?: 40,
                topP = (map["top_p"] as? Double)?.toFloat() ?: 0.95f,
                maxOutputTokens = (map["max_output_tokens"] as? Long)?.toInt() ?: 2048,
                systemPrompt = map["system_prompt"] as? String ?: "You are a helpful assistant.",
                isActive = map["is_active"] as? Boolean != false,
                version = map["version"] as? String ?: "1.0",
                updatedAt = map["updated_at"] as? Timestamp,
                availableModels = (map["available_models"] as? Map<String, String>) ?: emptyMap()
            )
        }
    }

    fun getCurrentModelDisplayName(): String {
        return availableModels.entries.find { it.value == modelName }?.key ?: modelName
    }
}