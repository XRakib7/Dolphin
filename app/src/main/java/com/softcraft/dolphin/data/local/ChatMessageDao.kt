package com.softcraft.dolphin.data.local

import androidx.room.*
import com.softcraft.dolphin.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chatmessage ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessage)

    @Query("DELETE FROM chatmessage")
    suspend fun deleteAll()

    @Query("SELECT * FROM chatmessage WHERE isUser = 1 ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentUserMessages(limit: Int): List<ChatMessage>
}