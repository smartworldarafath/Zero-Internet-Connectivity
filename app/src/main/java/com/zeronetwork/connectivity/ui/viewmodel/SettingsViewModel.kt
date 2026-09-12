package com.zeronetwork.connectivity.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeronetwork.connectivity.ZeroNetworkApp
import com.zeronetwork.connectivity.data.model.NetworkStats
import com.zeronetwork.connectivity.data.model.UpdateItem
import com.zeronetwork.connectivity.data.model.UserProfile
import com.zeronetwork.connectivity.network.GitHubAsset
import com.zeronetwork.connectivity.network.GitHubRelease
import com.zeronetwork.connectivity.network.UpdateDownloadState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

enum class SettingsTab(val title: String, val icon: String) {
    CHAT_SETTINGS("Chat Settings", "chat"),
    CONNECTIONS("Connections", "wifi"),
    APPEARANCE("Appearance", "palette"),
    UPDATES("Updates", "system_update"),
    APP_INFO("App Info", "info")
}

class SettingsViewModel : ViewModel() {
    private val app = ZeroNetworkApp.instance
    private val prefs = app.preferencesManager
    private val networkMonitor = app.networkMonitor
    private val discoveryService = app.discoveryService
    private val chatRepo = app.chatRepository
    val updateManager = app.updateManager

    val activeSettingsTab = MutableStateFlow(SettingsTab.CHAT_SETTINGS)
    val userProfile: StateFlow<UserProfile> = prefs.userProfile
    val networkStats: StateFlow<NetworkStats> = networkMonitor.networkStats

    val latestRelease: StateFlow<GitHubRelease?> = updateManager.latestRelease
    val isUpdateAvailable: StateFlow<Boolean> = updateManager.isUpdateAvailable
    val isCheckingUpdates: StateFlow<Boolean> = updateManager.isChecking
    val downloadState: StateFlow<UpdateDownloadState> = updateManager.downloadState

    val updateHistory = listOf(
        UpdateItem(
            version = "v1.0.1",
            releaseDate = "Current Release (September 2026)",
            title = "Subnet Discovery, QR Connect & Real GitHub Updates",
            changelog = listOf(
                "Multi-interface UDP broadcast & active subnet ping sweep",
                "QR Code generator and CameraX real-time QR scanner pairing",
                "Automatic saving of received photos and videos directly to system Gallery",
                "Telegram-style hold-to-record and release-to-send voice notes",
                "Real GitHub Releases updater with automatic install",
                "Connections diagnostics with refresh button, hardware MAC, and Wi-Fi frequency",
                "Collapsible 3-line slider navigation in Settings for full-width layout",
                "Signal & Telegram level chat customization (radius, wallpaper, reactions)",
                "Profile photo management (Camera, Gallery, Delete) with P2P persistence"
            ),
            isCurrent = true
        ),
        UpdateItem(
            version = "v1.0.0",
            releaseDate = "September 2026",
            title = "Zero Network Connectivity Launch",
            changelog = listOf(
                "Complete LAN peer-to-peer communication engine",
                "HD LAN Voice and Video Calling with hardware AEC",
                "High-speed multi-part file and photo sharing",
                "Telegram-style spring animations & live typing indicators",
                "Material 3 UI with 120Hz butter-smooth frame pacing"
            )
        )
    )

    fun selectTab(tab: SettingsTab) {
        activeSettingsTab.value = tab
    }

    fun refreshNetworkDiagnostics() {
        networkMonitor.refreshStats()
    }

    fun updateUsername(name: String) {
        val updated = userProfile.value.copy(username = name)
        prefs.updateProfile(updated)
        discoveryService.updateUsername(name)
    }

    fun updatePhoneNumber(phone: String) {
        prefs.updateProfile(userProfile.value.copy(phoneNumber = phone))
    }

    fun updateEmail(email: String) {
        prefs.updateProfile(userProfile.value.copy(email = email))
    }

    fun updateAvatarUri(uri: String?) {
        prefs.updateProfile(userProfile.value.copy(avatarUri = uri))
    }

    fun updateBubbleColor(key: String) {
        prefs.updateProfile(userProfile.value.copy(bubbleColorKey = key))
    }

    fun updateFontStyle(style: String) {
        prefs.updateProfile(userProfile.value.copy(fontStyleKey = style))
    }

    fun updateBubbleRadius(radius: Int) {
        prefs.updateProfile(userProfile.value.copy(bubbleRadiusDp = radius))
    }

    fun updateBubblePreset(preset: String) {
        prefs.updateProfile(userProfile.value.copy(bubblePreset = preset))
    }

    fun updateFontSizeScale(scale: Float) {
        prefs.updateProfile(userProfile.value.copy(fontSizeScale = scale))
    }

    fun updateWallpaper(pattern: String) {
        prefs.updateProfile(userProfile.value.copy(wallpaperPattern = pattern))
    }

    fun updateDoubleTapEmoji(emoji: String) {
        prefs.updateProfile(userProfile.value.copy(doubleTapEmoji = emoji))
    }

    fun toggleInAppSounds(enabled: Boolean) {
        prefs.updateProfile(userProfile.value.copy(inAppSounds = enabled))
    }

    fun toggleVibrationFeedback(enabled: Boolean) {
        prefs.updateProfile(userProfile.value.copy(vibrationFeedback = enabled))
    }

    fun toggleSwipeToReply(enabled: Boolean) {
        prefs.updateProfile(userProfile.value.copy(swipeToReply = enabled))
    }

    fun toggleEnterSends(enabled: Boolean) {
        prefs.updateProfile(userProfile.value.copy(enterSends = enabled))
    }

    fun toggleAutoDownloadPhotos(enabled: Boolean) {
        prefs.updateProfile(userProfile.value.copy(autoDownloadPhotos = enabled))
    }

    fun toggleAutoDownloadVideos(enabled: Boolean) {
        prefs.updateProfile(userProfile.value.copy(autoDownloadVideos = enabled))
    }

    fun toggleAutoDownloadFiles(enabled: Boolean) {
        prefs.updateProfile(userProfile.value.copy(autoDownloadFiles = enabled))
    }

    fun clearAllChatHistory() {
        viewModelScope.launch {
            chatRepo.clearAllHistory()
        }
    }

    fun checkForUpdates() {
        updateManager.checkForUpdates(silent = false)
    }

    fun downloadAndInstallUpdate(asset: GitHubAsset) {
        updateManager.downloadAndInstallUpdate(asset)
    }
}
