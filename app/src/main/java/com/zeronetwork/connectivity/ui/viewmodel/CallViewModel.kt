package com.zeronetwork.connectivity.ui.viewmodel

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.zeronetwork.connectivity.ZeroNetworkApp
import com.zeronetwork.connectivity.data.model.CallSession
import kotlinx.coroutines.flow.StateFlow

class CallViewModel : ViewModel() {
    private val app = ZeroNetworkApp.instance
    private val callEngine = app.callEngine

    val callSession: StateFlow<CallSession?> = callEngine.callSession
    val remoteVideoBitmap: StateFlow<Bitmap?> = callEngine.remoteVideoBitmap

    fun startCall(peerIp: String, peerName: String, isVideo: Boolean) {
        callEngine.startOutgoingCall(peerIp, peerName, isVideo)
    }

    fun acceptCall() {
        callEngine.acceptIncomingCall()
    }

    fun rejectCall() {
        callEngine.rejectIncomingCall()
    }

    fun endCall() {
        callEngine.endCall()
    }

    fun toggleMute() {
        callEngine.toggleMute()
    }

    fun toggleSpeaker() {
        callEngine.toggleSpeaker()
    }

    fun toggleCamera() {
        callEngine.toggleCamera()
    }

    fun switchCamera() {
        callEngine.switchCamera()
    }

    fun onCameraFrame(imageProxy: ImageProxy) {
        callEngine.processCameraFrame(imageProxy)
    }
}
