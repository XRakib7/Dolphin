package com.softcraft.dolphin.data.repository

import com.softcraft.dolphin.data.model.ChatMessage
import com.softcraft.dolphin.utils.GeminiHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val geminiHelper: GeminiHelper
) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    suspend fun sendMessage(message: String, config: com.softcraft.dolphin.data.model.AiConfig) {
        // Add user message
        val userMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            content = message,
            isUser = true
        )
        _messages.value = _messages.value + userMessage

        // Add loading AI message
        val loadingMessage = ChatMessage(
            id = "loading_${System.currentTimeMillis()}",
            content = "Thinking...",
            isUser = false
        )
        _messages.value = _messages.value + loadingMessage

        try {
            // Get AI response
            val response = geminiHelper.generateResponse(
                message = message,
                config = config
            )

            // Replace loading message with actual response
            _messages.value = _messages.value.map {
                if (it.id == loadingMessage.id) {
                    it.copy(content = response, isUser = false)
                } else {
                    it
                }
            }
        } catch (e: Exception) {
            // Replace loading message with error
            _messages.value = _messages.value.map {
                if (it.id == loadingMessage.id) {
                    it.copy(content = "Error: ${e.message}", isUser = false, error = true)
                } else {
                    it
                }
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }
}