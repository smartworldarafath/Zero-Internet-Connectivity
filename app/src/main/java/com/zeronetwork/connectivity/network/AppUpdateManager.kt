package com.zeronetwork.connectivity.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("body") val body: String = "",
    @SerializedName("published_at") val publishedAt: String = "",
    @SerializedName("html_url") val htmlUrl: String = "",
    @SerializedName("assets") val assets: List<GitHubAsset> = emptyList()
)

data class GitHubAsset(
    @SerializedName("name") val name: String = "",
    @SerializedName("browser_download_url") val downloadUrl: String = "",
    @SerializedName("size") val size: Long = 0L
)

sealed class UpdateDownloadState {
    object Idle : UpdateDownloadState()
    data class Downloading(val progress: Float, val speedKbps: Float, val downloadedBytes: Long, val totalBytes: Long) : UpdateDownloadState()
    data class Completed(val apkFile: File) : UpdateDownloadState()
    data class Failed(val error: String) : UpdateDownloadState()
}

class AppUpdateManager(private val context: Context) {
    private val TAG = "AppUpdateManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    val currentVersion = "v1.0.2"
    val releaseUrl = "https://github.com/smartworldarafath/Zero-Internet-Connectivity/releases"
    private val apiUrl = "https://api.github.com/repos/smartworldarafath/Zero-Internet-Connectivity/releases/latest"

    private val _latestRelease = MutableStateFlow<GitHubRelease?>(null)
    val latestRelease: StateFlow<GitHubRelease?> = _latestRelease.asStateFlow()

    private val _isUpdateAvailable = MutableStateFlow(false)
    val isUpdateAvailable: StateFlow<Boolean> = _isUpdateAvailable.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    private val _showHomeBanner = MutableStateFlow(false)
    val showHomeBanner: StateFlow<Boolean> = _showHomeBanner.asStateFlow()

    init {
        checkForUpdates(silent = true)
    }

    fun checkForUpdates(silent: Boolean = false) {
        scope.launch {
            _isChecking.value = true
            try {
                val url = URL(apiUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("User-Agent", "ZeroNetworkApp")
                    setRequestProperty("Accept", "application/vnd.github.v3+json")
                }

                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val release = gson.fromJson(responseText, GitHubRelease::class.java)
                    _latestRelease.value = release

                    val latestTag = release.tagName.trim()
                    if (isNewerVersion(latestTag, currentVersion)) {
                        _isUpdateAvailable.value = true
                        _showHomeBanner.value = true
                    } else {
                        _isUpdateAvailable.value = false
                    }
                } else {
                    Log.w(TAG, "GitHub API returned response code: ${conn.responseCode}")
                }
            } catch (e: Exception) {
                Log.d(TAG, "Offline or unable to reach GitHub releases: ${e.message}")
            } finally {
                _isChecking.value = false
            }
        }
    }

    fun dismissHomeBanner() {
        _showHomeBanner.value = false
    }

    fun downloadLatestRelease() {
        val rel = _latestRelease.value ?: return
        val asset = rel.assets.find { it.name.endsWith(".apk") } ?: rel.assets.firstOrNull()
        if (asset != null) {
            downloadAndInstallUpdate(asset)
        } else {
            // Open release browser page
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rel.htmlUrl.ifBlank { releaseUrl })).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {}
        }
    }

    fun downloadAndInstallUpdate(asset: GitHubAsset) {
        scope.launch {
            _downloadState.value = UpdateDownloadState.Downloading(0f, 0f, 0L, asset.size)
            try {
                val apkDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "updates").apply { mkdirs() }
                val apkFile = File(apkDir, "ZeroNetworkConnectivity-${_latestRelease.value?.tagName ?: "latest"}.apk")

                val url = URL(asset.downloadUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 15000
                    setRequestProperty("User-Agent", "ZeroNetworkApp")
                }

                val totalLength = if (conn.contentLengthLong > 0) conn.contentLengthLong else asset.size
                val bis = BufferedInputStream(conn.inputStream)
                val fos = FileOutputStream(apkFile)

                val buffer = ByteArray(8192)
                var totalRead = 0L
                var lastTime = System.currentTimeMillis()
                var bytesInterval = 0L

                var read: Int
                while (bis.read(buffer).also { read = it } != -1) {
                    fos.write(buffer, 0, read)
                    totalRead += read
                    bytesInterval += read

                    val now = System.currentTimeMillis()
                    val elapsed = now - lastTime
                    if (elapsed >= 300) {
                        val speed = (bytesInterval / 1024f) / (elapsed / 1000f)
                        val progress = if (totalLength > 0) totalRead.toFloat() / totalLength.toFloat() else 0.5f
                        _downloadState.value = UpdateDownloadState.Downloading(progress, speed, totalRead, totalLength)
                        lastTime = now
                        bytesInterval = 0L
                    }
                }
                fos.flush()
                fos.close()
                bis.close()

                _downloadState.value = UpdateDownloadState.Completed(apkFile)
                installApk(apkFile)
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading update: ${e.message}")
                _downloadState.value = UpdateDownloadState.Failed(e.message ?: "Download failed")
            }
        }
    }

    fun installApk(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching package installer: ${e.message}")
        }
    }

    private fun isNewerVersion(remoteTag: String, currentTag: String): Boolean {
        try {
            val rParts = remoteTag.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
            val cParts = currentTag.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }

            for (i in 0 until maxOf(rParts.size, cParts.size)) {
                val r = rParts.getOrElse(i) { 0 }
                val c = cParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
        } catch (e: Exception) {
            return remoteTag != currentTag
        }
        return false
    }
}
