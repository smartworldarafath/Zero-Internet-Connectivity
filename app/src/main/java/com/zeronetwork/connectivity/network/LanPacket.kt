package com.zeronetwork.connectivity.network

import com.google.gson.Gson
import com.zeronetwork.connectivity.data.model.MessageType
import java.nio.charset.StandardCharsets

data class RichPacket(
    val type: String, // "TYPING", "CALL_INVITE", "CALL_ACCEPT", "CALL_REJECT", "CALL_HANGUP", "GROUP_MSG", "FILE_META"
    val senderId: String,
    val senderName: String,
    val senderIp: String = "",
    val targetIp: String? = null,
    val isTyping: Boolean = false,
    val isVideo: Boolean = false,
    val callId: String? = null,
    val conversationId: String? = null,
    val textContent: String? = null,
    val fileName: String? = null,
    val fileSize: Long = 0L,
    val filePort: Int = NetworkConstants.TCP_FILE_PORT,
    val timestamp: Long = System.currentTimeMillis()
)

object LanPacketHelper {
    private val gson = Gson()

    fun createHeartbeatPacket(username: String): ByteArray {
        val bytes = username.toByteArray(StandardCharsets.UTF_8)
        val packet = ByteArray(1 + bytes.size)
        packet[0] = NetworkConstants.TYPE_HEARTBEAT
        System.arraycopy(bytes, 0, packet, 1, bytes.size)
        return packet
    }

    fun createTextPacket(text: String): ByteArray {
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        val packet = ByteArray(1 + bytes.size)
        packet[0] = NetworkConstants.TYPE_TEXT
        System.arraycopy(bytes, 0, packet, 1, bytes.size)
        return packet
    }

    fun createRichPacket(packet: RichPacket): ByteArray {
        val json = gson.toJson(packet)
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        val res = ByteArray(1 + bytes.size)
        res[0] = NetworkConstants.TYPE_RICH_SIGNAL
        System.arraycopy(bytes, 0, res, 1, bytes.size)
        return res
    }

    fun parseRichPacket(data: ByteArray, length: Int): RichPacket? {
        if (length <= 1) return null
        return try {
            val json = String(data, 1, length - 1, StandardCharsets.UTF_8)
            gson.fromJson(json, RichPacket::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun createOwnAddressPacket(identifier: Byte): ByteArray {
        val packet = ByteArray(11)
        packet[0] = NetworkConstants.TYPE_OWN_ADDR
        for (i in 1..10) {
            packet[i] = identifier
        }
        return packet
    }

    fun isOwnAddressPacket(data: ByteArray, length: Int, identifier: Byte): Boolean {
        if (length < 11 || data[0] != NetworkConstants.TYPE_OWN_ADDR) return false
        for (i in 1..10) {
            if (data[i] != identifier) return false
        }
        return true
    }
}
