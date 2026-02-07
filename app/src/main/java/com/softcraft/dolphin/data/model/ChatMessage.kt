package com.softcraft.dolphin.data.model

import java.util.Date

data class ChatMessage(
    val id: String = "",
    val content: String = "",
    val isUser: Boolean = true,
    val timestamp: Date = Date(),
    val error: Boolean = false
)