package com.zeronetwork.connectivity.data.model

data class NetworkStats(
    val ssid: String = "Disconnected",
    val bssid: String = "00:00:00:00:00:00",
    val macAddress: String = "02:00:00:00:00:00",
    val ipAddress: String = "127.0.0.1",
    val subnetMask: String = "255.255.255.0",
    val gateway: String = "192.168.1.1",
    val rssi: Int = 0,
    val signalStrength: Int = 0,
    val linkSpeedMbps: Int = 0,
    val frequencyMhz: Int = 0,
    val isConnectedToWifi: Boolean = false,
    val activePeersCount: Int = 0
)
