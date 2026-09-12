package com.zeronetwork.connectivity.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ChatConversation(
    @PrimaryKey
    val id: String, // IP for 1:1 or UUID for group
    val title: String,
    val isGroup: Boolean = false,
    val participantIps: String = "", // Comma-separated IPs
    val lastMessageSnippet: String = "",
    val lastMessageType: MessageType = MessageType.TEXT,
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val avatarColorIndex: Int = 0,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val adminIp: String? = null
)
