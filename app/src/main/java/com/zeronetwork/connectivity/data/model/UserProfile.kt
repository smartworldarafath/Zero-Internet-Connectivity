package com.zeronetwork.connectivity.data.model

data class UserProfile(
    val username: String = "My Device",
    val phoneNumber: String = "+1 555-0199",
    val email: String = "user@zeronetwork.lan",
    val avatarUri: String? = null,
    val avatarColorIndex: Int = 0,
    val storageMode: String = "LOCAL", // "LOCAL" or "CLOUD"
    val bubbleColorKey: String = "SIGNAL_BLUE", // "SIGNAL_BLUE", "EMERALD", "PURPLE", "SUNSET", "MIDNIGHT"
    val fontStyleKey: String = "DEFAULT", // "DEFAULT", "INTER", "MONO", "SERIF"
    val enterSends: Boolean = true,
    val autoDownloadPhotos: Boolean = true,
    val autoDownloadVideos: Boolean = false,
    val autoDownloadFiles: Boolean = true,
    val bubbleRadiusDp: Int = 18,
    val bubblePreset: String = "TELEGRAM", // "TELEGRAM", "SIGNAL", "MODERN", "MINIMAL"
    val fontSizeScale: Float = 1.0f,
    val inAppSounds: Boolean = true,
    val vibrationFeedback: Boolean = true,
    val wallpaperPattern: String = "DEFAULT", // "DEFAULT", "DOODLE", "SOLID_DARK", "SOLID_LIGHT", "GRADIENT"
    val doubleTapEmoji: String = "❤️",
    val swipeToReply: Boolean = true
)
