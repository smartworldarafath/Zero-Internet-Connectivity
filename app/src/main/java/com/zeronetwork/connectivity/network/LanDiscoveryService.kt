package com.zeronetwork.connectivity.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

sealed class LanEvent {
    data class PeerHeartbeat(val ipAddress: String, val username: String, val lastSeen: Long, val avatarBase64: String? = null) : LanEvent()
    data class TextMessage(val senderIp: String, val text: String, val timestamp: Long) : LanEvent()
    data class FileChunkReceived(val senderIp: String, val fileName: String, val chunkIndex: Int, val totalChunks: Int, val progress: Float) : LanEvent()
    data class FileCompleted(val senderIp: String, val fileName: String, val filePath: String, val fileSize: Long) : LanEvent()
    data class TypingEvent(val senderIp: String, val isTyping: Boolean) : LanEvent()
    data class CallSignal(val signal: RichPacket) : LanEvent()
    data class AvatarSyncEvent(val senderIp: String, val avatarBase64: String) : LanEvent()
    data class GroupUpdateEvent(val groupId: String, val groupTitle: String, val memberIps: String) : LanEvent()
}

class LanDiscoveryService(private val context: Context) {
    private val TAG = "LanDiscoveryService"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var socket: DatagramSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private val ownAddressIdentifier = Random.nextInt(1, 100).toByte()
    private val _ownIpAddress = MutableStateFlow<String?>("127.0.0.1")
    val ownIpAddress: StateFlow<String?> = _ownIpAddress.asStateFlow()

    private val _events = MutableSharedFlow<LanEvent>(extraBufferCapacity = 128)
    val events: SharedFlow<LanEvent> = _events.asSharedFlow()

    private var heartbeatJob: Job? = null
    private var receiveJob: Job? = null

    private class IncompleteFile(
        val fileName: String,
        val totalChunks: Int,
        val chunks: ConcurrentHashMap<Int, ByteArray> = ConcurrentHashMap()
    )
    private val incompleteFiles = ConcurrentHashMap<String, IncompleteFile>()

    private var currentUsername: String = "User"
    private var currentAvatarBase64: String? = null

    fun start(username: String, avatarBase64: String? = null) {
        currentUsername = username
        currentAvatarBase64 = avatarBase64
        acquireLocks()
        initSocket()
        startListening()
        startHeartbeat()
        triggerSubnetScan()
    }

    private fun acquireLocks() {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                multicastLock = wifiManager.createMulticastLock("ZeroNetworkMulticastLock").apply {
                    setReferenceCounted(false)
                    acquire()
                }
                @Suppress("DEPRECATION")
                wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "ZeroNetworkWifiLock").apply {
                    acquire()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring locks: ${e.message}")
        }
    }

    private fun initSocket() {
        try {
            socket?.close()
            socket = DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                bind(InetSocketAddress(NetworkConstants.DISCOVERY_PORT))
            }
            Log.d(TAG, "UDP Socket bound to port ${NetworkConstants.DISCOVERY_PORT}")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating UDP socket: ${e.message}")
        }
    }

    private fun startListening() {
        receiveJob?.cancel()
        receiveJob = scope.launch {
            val buffer = ByteArray(65535)
            while (isActive) {
                try {
                    val datagramSocket = socket ?: break
                    val packet = DatagramPacket(buffer, buffer.size)
                    datagramSocket.receive(packet)

                    val senderAddress = packet.address.hostAddress ?: continue
                    val length = packet.length
                    if (length == 0) continue

                    val firstByte = buffer[0]

                    if (LanPacketHelper.isOwnAddressPacket(buffer, length, ownAddressIdentifier)) {
                        _ownIpAddress.value = senderAddress
                        continue
                    }

                    if (senderAddress == _ownIpAddress.value || senderAddress == "127.0.0.1") {
                        continue
                    }

                    when (firstByte) {
                        NetworkConstants.TYPE_HEARTBEAT -> {
                            val username = if (length > 1) {
                                String(buffer, 1, length - 1, StandardCharsets.UTF_8)
                            } else "Peer Device"
                            _events.emit(LanEvent.PeerHeartbeat(senderAddress, username, System.currentTimeMillis()))
                        }

                        NetworkConstants.TYPE_TEXT -> {
                            val text = if (length > 1) {
                                String(buffer, 1, length - 1, StandardCharsets.UTF_8)
                            } else ""
                            _events.emit(LanEvent.TextMessage(senderAddress, text, System.currentTimeMillis()))
                        }

                        NetworkConstants.TYPE_FILE_CHUNK -> {
                            if (length > 4) {
                                val nameLength = buffer[1].toInt() and 0xFF
                                val chunkIndex = buffer[2].toInt() and 0xFF
                                val totalChunks = buffer[3].toInt() and 0xFF
                                if (length >= 4 + nameLength) {
                                    val fileName = String(buffer, 4, nameLength, StandardCharsets.UTF_8)
                                    val dataStart = 4 + nameLength
                                    val dataLen = length - dataStart
                                    val chunkData = ByteArray(dataLen)
                                    System.arraycopy(buffer, dataStart, chunkData, 0, dataLen)

                                    handleFileChunk(senderAddress, fileName, chunkIndex, totalChunks, chunkData)
                                }
                            }
                        }

                        NetworkConstants.TYPE_RICH_SIGNAL -> {
                            val richPacket = LanPacketHelper.parseRichPacket(buffer, length)
                            if (richPacket != null) {
                                when (richPacket.type) {
                                    "TYPING" -> {
                                        _events.emit(LanEvent.TypingEvent(senderAddress, richPacket.isTyping))
                                    }
                                    "CALL_INVITE", "CALL_ACCEPT", "CALL_REJECT", "CALL_HANGUP", "CALL_RINGING" -> {
                                        _events.emit(LanEvent.CallSignal(richPacket.copy(senderIp = senderAddress)))
                                    }
                                    "PEER_PROBE" -> {
                                        _events.emit(LanEvent.PeerHeartbeat(senderAddress, richPacket.senderName, System.currentTimeMillis(), richPacket.avatarBase64))
                                        sendDirectHeartbeat(senderAddress)
                                    }
                                    "AVATAR_UPDATE" -> {
                                        richPacket.avatarBase64?.let {
                                            _events.emit(LanEvent.AvatarSyncEvent(senderAddress, it))
                                        }
                                    }
                                    "GROUP_UPDATE" -> {
                                        if (richPacket.conversationId != null && richPacket.groupTitle != null && richPacket.memberIps != null) {
                                            _events.emit(LanEvent.GroupUpdateEvent(richPacket.conversationId, richPacket.groupTitle, richPacket.memberIps))
                                        }
                                    }
                                    else -> {
                                        if (!richPacket.textContent.isNullOrBlank()) {
                                            _events.emit(LanEvent.TextMessage(senderAddress, richPacket.textContent, richPacket.timestamp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) delay(100)
                }
            }
        }
    }

    private suspend fun handleFileChunk(senderIp: String, fileName: String, chunkIndex: Int, totalChunks: Int, chunkData: ByteArray) {
        val fileKey = "${senderIp}_$fileName"
        val incomplete = incompleteFiles.getOrPut(fileKey) {
            IncompleteFile(fileName, totalChunks)
        }

        incomplete.chunks[chunkIndex] = chunkData
        val progress = incomplete.chunks.size.toFloat() / totalChunks.toFloat()
        _events.emit(LanEvent.FileChunkReceived(senderIp, fileName, chunkIndex, totalChunks, progress))

        if (incomplete.chunks.size == totalChunks) {
            val receivedDir = File(context.filesDir, "received_files").apply { mkdirs() }
            val outputFile = File(receivedDir, "${System.currentTimeMillis()}_$fileName")
            FileOutputStream(outputFile).use { fos ->
                for (i in 1..totalChunks) {
                    incomplete.chunks[i]?.let { fos.write(it) }
                }
            }
            incompleteFiles.remove(fileKey)
            _events.emit(LanEvent.FileCompleted(senderIp, fileName, outputFile.absolutePath, outputFile.length()))
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            sendOwnAddressProbe()
            while (isActive) {
                sendHeartbeat()
                delay(NetworkConstants.HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    fun updateUsername(newUsername: String) {
        currentUsername = newUsername
        scope.launch { sendHeartbeat() }
    }

    fun updateAvatar(avatarBase64: String?) {
        currentAvatarBase64 = avatarBase64
        scope.launch {
            try {
                val packet = RichPacket(
                    type = "AVATAR_UPDATE",
                    senderId = _ownIpAddress.value ?: "me",
                    senderName = currentUsername,
                    avatarBase64 = avatarBase64
                )
                val data = LanPacketHelper.createRichPacket(packet)
                sendToAllBroadcastTargets(data)
            } catch (e: Exception) {}
        }
    }

    private fun sendOwnAddressProbe() {
        scope.launch {
            try {
                val probeData = LanPacketHelper.createOwnAddressPacket(ownAddressIdentifier)
                sendToAllBroadcastTargets(probeData)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending own address probe: ${e.message}")
            }
        }
    }

    fun sendHeartbeat() {
        scope.launch {
            try {
                val heartbeatData = LanPacketHelper.createHeartbeatPacket(currentUsername)
                sendToAllBroadcastTargets(heartbeatData)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending heartbeat: ${e.message}")
            }
        }
    }

    fun sendDirectHeartbeat(targetIp: String) {
        scope.launch {
            try {
                val heartbeatData = LanPacketHelper.createHeartbeatPacket(currentUsername)
                val targetAddr = InetAddress.getByName(targetIp)
                val packet = DatagramPacket(heartbeatData, heartbeatData.size, targetAddr, NetworkConstants.DISCOVERY_PORT)
                socket?.send(packet)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending direct heartbeat: ${e.message}")
            }
        }
    }

    fun probePeer(targetIp: String) {
        scope.launch {
            try {
                val richPacket = RichPacket(
                    type = "PEER_PROBE",
                    senderId = _ownIpAddress.value ?: "me",
                    senderName = currentUsername,
                    targetIp = targetIp,
                    avatarBase64 = currentAvatarBase64
                )
                val data = LanPacketHelper.createRichPacket(richPacket)
                val targetAddr = InetAddress.getByName(targetIp)
                val packet = DatagramPacket(data, data.size, targetAddr, NetworkConstants.DISCOVERY_PORT)
                socket?.send(packet)
            } catch (e: Exception) {}
        }
    }

    fun triggerSubnetScan() {
        scope.launch(Dispatchers.IO) {
            try {
                sendHeartbeat()
                val localIp = _ownIpAddress.value ?: return@launch
                if (localIp == "127.0.0.1" || !localIp.contains(".")) return@launch
                val prefix = localIp.substringBeforeLast(".") + "."
                val heartbeatData = LanPacketHelper.createHeartbeatPacket(currentUsername)

                val jobs = (1..254).map { i ->
                    launch {
                        try {
                            val ip = "$prefix$i"
                            if (ip != localIp) {
                                val addr = InetAddress.getByName(ip)
                                val pkt = DatagramPacket(heartbeatData, heartbeatData.size, addr, NetworkConstants.DISCOVERY_PORT)
                                socket?.send(pkt)
                            }
                        } catch (e: Exception) {}
                    }
                }
                jobs.forEach { it.join() }
            } catch (e: Exception) {
                Log.e(TAG, "Subnet scan error: ${e.message}")
            }
        }
    }

    private fun sendToAllBroadcastTargets(data: ByteArray) {
        val targets = mutableSetOf<InetAddress>()
        try {
            targets.add(InetAddress.getByName(NetworkConstants.BROADCAST_ADDRESS))
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (intf.isLoopback || !intf.isUp) continue
                for (ifaceAddr in intf.interfaceAddresses) {
                    val bcast = ifaceAddr.broadcast
                    if (bcast != null) {
                        targets.add(bcast)
                    }
                }
            }
        } catch (e: Exception) {}

        for (target in targets) {
            try {
                val packet = DatagramPacket(data, data.size, target, NetworkConstants.DISCOVERY_PORT)
                socket?.send(packet)
            } catch (e: Exception) {}
        }
    }

    fun sendBroadcastText(text: String) {
        scope.launch {
            try {
                val textData = LanPacketHelper.createTextPacket(text)
                sendToAllBroadcastTargets(textData)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending broadcast text: ${e.message}")
            }
        }
    }

    fun sendDirectText(targetIp: String, text: String) {
        scope.launch {
            try {
                val textData = LanPacketHelper.createTextPacket(text)
                val targetAddr = InetAddress.getByName(targetIp)
                val packet = DatagramPacket(textData, textData.size, targetAddr, NetworkConstants.DISCOVERY_PORT)
                socket?.send(packet)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending direct text to $targetIp: ${e.message}")
            }
        }
    }

    fun sendTypingIndicator(targetIp: String?, isTyping: Boolean) {
        scope.launch {
            try {
                val richPacket = RichPacket(
                    type = "TYPING",
                    senderId = _ownIpAddress.value ?: "me",
                    senderName = currentUsername,
                    isTyping = isTyping,
                    targetIp = targetIp
                )
                val packetData = LanPacketHelper.createRichPacket(richPacket)
                if (targetIp != null) {
                    val targetAddr = InetAddress.getByName(targetIp)
                    val packet = DatagramPacket(packetData, packetData.size, targetAddr, NetworkConstants.DISCOVERY_PORT)
                    socket?.send(packet)
                } else {
                    sendToAllBroadcastTargets(packetData)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending typing indicator: ${e.message}")
            }
        }
    }

    fun sendGroupUpdate(groupId: String, groupTitle: String, memberIps: String) {
        scope.launch {
            try {
                val richPacket = RichPacket(
                    type = "GROUP_UPDATE",
                    senderId = _ownIpAddress.value ?: "me",
                    senderName = currentUsername,
                    conversationId = groupId,
                    groupTitle = groupTitle,
                    memberIps = memberIps
                )
                val data = LanPacketHelper.createRichPacket(richPacket)
                sendToAllBroadcastTargets(data)
            } catch (e: Exception) {}
        }
    }

    fun sendCallSignal(targetIp: String, packet: RichPacket) {
        scope.launch {
            try {
                val packetData = LanPacketHelper.createRichPacket(packet)
                val targetAddr = InetAddress.getByName(targetIp)
                val datagram = DatagramPacket(packetData, packetData.size, targetAddr, NetworkConstants.DISCOVERY_PORT)
                socket?.send(datagram)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending call signal: ${e.message}")
            }
        }
    }

    fun stop() {
        heartbeatJob?.cancel()
        receiveJob?.cancel()
        socket?.close()
        socket = null
        try {
            if (multicastLock?.isHeld == true) multicastLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (e: Exception) {}
    }
}
