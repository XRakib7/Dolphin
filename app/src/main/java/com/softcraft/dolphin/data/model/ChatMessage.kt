package com.softcraft.dolphin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chatmessage")
data class ChatMessage(
    @PrimaryKey
    val id: String = "",
    val content: String = "",
    val isUser: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(), // Change to Long
    val error: Boolean = false,
    val conversationId: String = "default"
)