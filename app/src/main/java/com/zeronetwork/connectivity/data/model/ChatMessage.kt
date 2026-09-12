package com.zeronetwork.connectivity.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "messages")
data class ChatMessage(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderAddress: String,
    val type: MessageType = MessageType.TEXT,
    val content: String = "",
    val filePath: String? = null,
    val fileName: String? = null,
    val fileSize: Long = 0L,
    val durationMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val isMine: Boolean = false,
    val status: MessageStatus = MessageStatus.SENT,
    val transferProgress: Float = 1.0f
)
