package com.softcraft.dolphin.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.softcraft.dolphin.data.model.AiConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val configCollection = firestore.collection("app_config")

    fun getAiConfigStream(): Flow<AiConfig> = callbackFlow {
        val listener = configCollection.document("ai_configuration")
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    if (it.exists()) {
                        val config = AiConfig.fromMap(it.data ?: emptyMap())
                        trySend(config)
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun updateModel(modelValue: String): Boolean {
        return try {
            configCollection.document("ai_configuration")
                .update(
                    mapOf(
                        "model_name" to modelValue,
                        "updated_at" to Timestamp.now()
                    )
                )
                .await()
            true
        } catch (_: Exception) {
            false
        }
    }
}