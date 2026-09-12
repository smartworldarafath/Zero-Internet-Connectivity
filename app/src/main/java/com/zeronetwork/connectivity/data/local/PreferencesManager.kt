package com.zeronetwork.connectivity.data.local

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.zeronetwork.connectivity.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("zero_network_prefs", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private fun loadProfile(): UserProfile {
        val defaultName = Build.MODEL?.takeIf { it.isNotBlank() } ?: "Android Device"
        return UserProfile(
            username = prefs.getString("username", defaultName) ?: defaultName,
            phoneNumber = prefs.getString("phoneNumber", "") ?: "",
            email = prefs.getString("email", "") ?: "",
            avatarUri = prefs.getString("avatarUri", null),
            avatarColorIndex = prefs.getInt("avatarColorIndex", 0),
            storageMode = prefs.getString("storageMode", "LOCAL") ?: "LOCAL",
            bubbleColorKey = prefs.getString("bubbleColorKey", "SIGNAL_BLUE") ?: "SIGNAL_BLUE",
            fontStyleKey = prefs.getString("fontStyleKey", "DEFAULT") ?: "DEFAULT",
            enterSends = prefs.getBoolean("enterSends", true),
            autoDownloadPhotos = prefs.getBoolean("autoDownloadPhotos", true),
            autoDownloadVideos = prefs.getBoolean("autoDownloadVideos", false),
            autoDownloadFiles = prefs.getBoolean("autoDownloadFiles", true),
            bubbleRadiusDp = prefs.getInt("bubbleRadiusDp", 18),
            bubblePreset = prefs.getString("bubblePreset", "TELEGRAM") ?: "TELEGRAM",
            fontSizeScale = prefs.getFloat("fontSizeScale", 1.0f),
            inAppSounds = prefs.getBoolean("inAppSounds", true),
            vibrationFeedback = prefs.getBoolean("vibrationFeedback", true),
            wallpaperPattern = prefs.getString("wallpaperPattern", "DEFAULT") ?: "DEFAULT",
            doubleTapEmoji = prefs.getString("doubleTapEmoji", "❤️") ?: "❤️",
            swipeToReply = prefs.getBoolean("swipeToReply", true)
        )
    }

    fun updateProfile(profile: UserProfile) {
        prefs.edit().apply {
            putString("username", profile.username)
            putString("phoneNumber", profile.phoneNumber)
            putString("email", profile.email)
            putString("avatarUri", profile.avatarUri)
            putInt("avatarColorIndex", profile.avatarColorIndex)
            putString("storageMode", profile.storageMode)
            putString("bubbleColorKey", profile.bubbleColorKey)
            putString("fontStyleKey", profile.fontStyleKey)
            putBoolean("enterSends", profile.enterSends)
            putBoolean("autoDownloadPhotos", profile.autoDownloadPhotos)
            putBoolean("autoDownloadVideos", profile.autoDownloadVideos)
            putBoolean("autoDownloadFiles", profile.autoDownloadFiles)
            putInt("bubbleRadiusDp", profile.bubbleRadiusDp)
            putString("bubblePreset", profile.bubblePreset)
            putFloat("fontSizeScale", profile.fontSizeScale)
            putBoolean("inAppSounds", profile.inAppSounds)
            putBoolean("vibrationFeedback", profile.vibrationFeedback)
            putString("wallpaperPattern", profile.wallpaperPattern)
            putString("doubleTapEmoji", profile.doubleTapEmoji)
            putBoolean("swipeToReply", profile.swipeToReply)
            apply()
        }
        _userProfile.value = profile
    }

    fun getDeviceId(): String {
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString().substring(0, 8)
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }

    fun getUsername(): String = _userProfile.value.username

    fun isFirstLaunch(): Boolean {
        val first = prefs.getBoolean("is_first_launch", true)
        if (first) {
            prefs.edit().putBoolean("is_first_launch", false).apply()
        }
        return first
    }
}
