package com.softcraft.dolphin.data.repository

import com.softcraft.dolphin.data.model.ChatMessage
import com.softcraft.dolphin.utils.GeminiHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val geminiHelper: GeminiHelper
) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    // Keep conversation history in memory (simple approach)
    private val conversationHistory = mutableListOf<ChatMessage>()
    private var maxHistoryItems = 10

    suspend fun sendMessage(message: String, config: com.softcraft.dolphin.data.model.AiConfig) {
        // Add user message
        val userMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            content = message,
            isUser = true
        )

        _messages.update { currentMessages ->
            currentMessages + userMessage
        }
        conversationHistory.add(userMessage)

        // Add loading message
        val loadingMessage = ChatMessage(
            id = "loading_${System.currentTimeMillis()}",
            content = "Thinking...",
            isUser = false
        )

        _messages.update { currentMessages ->
            currentMessages + loadingMessage
        }

        try {
            // Get formatted history for context
            val historyForContext = conversationHistory.takeLast(maxHistoryItems)

            // Get AI response WITH conversation context
            val response = geminiHelper.generateResponse(
                message = message,
                config = config,
                conversationHistory = historyForContext
            )

            // Create AI response
            val aiMessage = ChatMessage(
                id = (System.currentTimeMillis() + 1).toString(),
                content = response,
                isUser = false
            )

            // Update UI - replace loading message
            _messages.update { currentMessages ->
                currentMessages.filterNot { it.id == loadingMessage.id } + aiMessage
            }

            // Add to conversation history
            conversationHistory.add(aiMessage)

            // Trim history if too long
            if (conversationHistory.size > maxHistoryItems * 2) {
                val itemsToRemove = conversationHistory.size - (maxHistoryItems * 2)
                repeat(itemsToRemove) {
                    conversationHistory.removeAt(0)
                }
            }

        } catch (e: Exception) {
            // Handle error
            _messages.update { currentMessages ->
                currentMessages.map {
                    if (it.id == loadingMessage.id) {
                        it.copy(content = "Error: ${e.message}", isUser = false, error = true)
                    } else {
                        it
                    }
                }
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
        conversationHistory.clear()
    }

    fun setMaxHistoryItems(maxItems: Int) {
        maxHistoryItems = maxItems
    }
}