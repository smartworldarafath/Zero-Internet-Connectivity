package com.zeronetwork.connectivity.data.model

data class CallSession(
    val callId: String = "",
    val peerIp: String = "",
    val peerName: String = "Peer",
    val isVideo: Boolean = false,
    val isOutgoing: Boolean = true,
    val state: CallState = CallState.IDLE,
    val startTime: Long = 0L,
    val durationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isCameraEnabled: Boolean = true,
    val isFrontCamera: Boolean = true
)
