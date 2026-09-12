package com.zeronetwork.connectivity.network

object NetworkConstants {
    const val DISCOVERY_PORT = 1050
    const val TCP_FILE_PORT = 1051
    const val VOICE_CALL_PORT = 1052
    const val VIDEO_CALL_PORT = 1053

    const val HEARTBEAT_INTERVAL_MS = 4000L
    const val PEER_TIMEOUT_MS = 12000L
    const val BROADCAST_ADDRESS = "255.255.255.255"

    // Packet type markers
    const val TYPE_HEARTBEAT: Byte = 0
    const val TYPE_TEXT: Byte = 1
    const val TYPE_FILE_CHUNK: Byte = 2
    const val TYPE_OWN_ADDR: Byte = 4
    const val TYPE_RICH_SIGNAL: Byte = 10
}
