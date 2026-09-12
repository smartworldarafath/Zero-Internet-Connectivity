package com.zeronetwork.connectivity.data.repository

import com.zeronetwork.connectivity.data.local.PeerDao
import com.zeronetwork.connectivity.data.model.Peer
import com.zeronetwork.connectivity.network.LanDiscoveryService
import com.zeronetwork.connectivity.network.LanEvent
import com.zeronetwork.connectivity.network.NetworkConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PeerRepository(
    private val peerDao: PeerDao,
    private val discoveryService: LanDiscoveryService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var cleanupJob: Job? = null

    val onlinePeers: Flow<List<Peer>> = peerDao.getOnlinePeers()
    val allPeers: Flow<List<Peer>> = peerDao.getAllPeers()

    init {
        listenToDiscoveryEvents()
        startPeriodicPeerCleanup()
    }

    private fun listenToDiscoveryEvents() {
        scope.launch {
            discoveryService.events.collect { event ->
                when (event) {
                    is LanEvent.PeerHeartbeat -> {
                        val existing = peerDao.getPeerByIp(event.ipAddress)
                        val colorIdx = existing?.avatarColorIndex ?: (event.ipAddress.hashCode().mod(6).let { if (it < 0) -it else it })
                        val updatedPeer = Peer(
                            ipAddress = event.ipAddress,
                            username = event.username,
                            avatarColorIndex = colorIdx,
                            avatarBase64 = event.avatarBase64 ?: existing?.avatarBase64,
                            lastSeen = event.lastSeen,
                            isOnline = true,
                            isTyping = existing?.isTyping ?: false
                        )
                        peerDao.insertPeer(updatedPeer)
                    }
                    is LanEvent.AvatarSyncEvent -> {
                        val existing = peerDao.getPeerByIp(event.senderIp)
                        if (existing != null) {
                            peerDao.insertPeer(existing.copy(avatarBase64 = event.avatarBase64))
                        }
                    }
                    is LanEvent.TypingEvent -> {
                        peerDao.setPeerTyping(event.senderIp, event.isTyping)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun startPeriodicPeerCleanup() {
        cleanupJob?.cancel()
        cleanupJob = scope.launch {
            while (isActive) {
                delay(5000)
                val cutoff = System.currentTimeMillis() - NetworkConstants.PEER_TIMEOUT_MS
                peerDao.markStalePeersOffline(cutoff)
            }
        }
    }

    suspend fun getPeerByIp(ip: String): Peer? = peerDao.getPeerByIp(ip)

    fun sendTyping(targetIp: String?, isTyping: Boolean) {
        discoveryService.sendTypingIndicator(targetIp, isTyping)
    }
}
