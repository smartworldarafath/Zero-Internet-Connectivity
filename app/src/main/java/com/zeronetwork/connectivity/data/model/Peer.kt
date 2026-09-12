package com.zeronetwork.connectivity.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "peers")
data class Peer(
    @PrimaryKey
    val ipAddress: String,
    val username: String = "User",
    val port: Int = 1050,
    val tcpPort: Int = 1051,
    val avatarColorIndex: Int = 0,
    val avatarBase64: String? = null,
    val lastSeen: Long = System.currentTimeMillis(),
    val isOnline: Boolean = true,
    val isTyping: Boolean = false,
    val signalStrength: Int = 100,
    val deviceModel: String = "Android Device"
)
