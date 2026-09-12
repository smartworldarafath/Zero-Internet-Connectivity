package com.zeronetwork.connectivity.network

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import androidx.camera.core.ImageProxy
import com.zeronetwork.connectivity.data.model.CallSession
import com.zeronetwork.connectivity.data.model.CallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer

class LanCallEngine(private val context: Context, private val discoveryService: LanDiscoveryService) {
    private val TAG = "LanCallEngine"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _callSession = MutableStateFlow<CallSession?>(null)
    val callSession: StateFlow<CallSession?> = _callSession.asStateFlow()

    private val _remoteVideoBitmap = MutableStateFlow<Bitmap?>(null)
    val remoteVideoBitmap: StateFlow<Bitmap?> = _remoteVideoBitmap.asStateFlow()

    // Audio components
    private val sampleRate = 16000
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSizeAudioIn = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat).coerceAtLeast(1024)
    private val bufferSizeAudioOut = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat).coerceAtLeast(1024)

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null

    private var audioSocket: DatagramSocket? = null
    private var videoSocket: DatagramSocket? = null

    private var audioRecordJob: Job? = null
    private var audioPlayJob: Job? = null
    private var videoReceiveJob: Job? = null
    private var timerJob: Job? = null

    init {
        // Listen to call signals from discovery service
        scope.launch {
            discoveryService.events.collect { event ->
                if (event is LanEvent.CallSignal) {
                    handleIncomingSignal(event.signal)
                }
            }
        }
    }

    private fun handleIncomingSignal(signal: RichPacket) {
        when (signal.type) {
            "CALL_INVITE" -> {
                val current = _callSession.value
                if (current != null && current.state == CallState.CONNECTED) {
                    // Busy
                    discoveryService.sendCallSignal(
                        signal.senderIp,
                        RichPacket(
                            type = "CALL_REJECT",
                            senderId = discoveryService.ownIpAddress.value ?: "",
                            senderName = "",
                            callId = signal.callId
                        )
                    )
                    return
                }

                _callSession.value = CallSession(
                    callId = signal.callId ?: System.currentTimeMillis().toString(),
                    peerIp = signal.senderIp,
                    peerName = signal.senderName,
                    isVideo = signal.isVideo,
                    isOutgoing = false,
                    state = CallState.INCOMING_RINGING
                )
            }
            "CALL_ACCEPT" -> {
                val current = _callSession.value
                if (current != null && current.peerIp == signal.senderIp) {
                    _callSession.value = current.copy(
                        state = CallState.CONNECTED,
                        startTime = System.currentTimeMillis()
                    )
                    startCallStreaming(signal.senderIp, current.isVideo)
                    startCallTimer()
                }
            }
            "CALL_REJECT" -> {
                val current = _callSession.value
                if (current != null && current.peerIp == signal.senderIp) {
                    _callSession.value = current.copy(state = CallState.REJECTED)
                    scope.launch {
                        delay(2000)
                        endCall(sendHangup = false)
                    }
                }
            }
            "CALL_HANGUP" -> {
                val current = _callSession.value
                if (current != null && current.peerIp == signal.senderIp) {
                    _callSession.value = current.copy(state = CallState.ENDED)
                    scope.launch {
                        delay(1500)
                        endCall(sendHangup = false)
                    }
                }
            }
        }
    }

    fun startOutgoingCall(peerIp: String, peerName: String, isVideo: Boolean) {
        val callId = System.currentTimeMillis().toString()
        _callSession.value = CallSession(
            callId = callId,
            peerIp = peerIp,
            peerName = peerName,
            isVideo = isVideo,
            isOutgoing = true,
            state = CallState.OUTGOING_CALLING
        )

        discoveryService.sendCallSignal(
            peerIp,
            RichPacket(
                type = "CALL_INVITE",
                senderId = discoveryService.ownIpAddress.value ?: "",
                senderName = discoveryService.ownIpAddress.value ?: "User",
                callId = callId,
                isVideo = isVideo
            )
        )
    }

    fun acceptIncomingCall() {
        val session = _callSession.value ?: return
        _callSession.value = session.copy(
            state = CallState.CONNECTED,
            startTime = System.currentTimeMillis()
        )

        discoveryService.sendCallSignal(
            session.peerIp,
            RichPacket(
                type = "CALL_ACCEPT",
                senderId = discoveryService.ownIpAddress.value ?: "",
                senderName = "",
                callId = session.callId
            )
        )

        startCallStreaming(session.peerIp, session.isVideo)
        startCallTimer()
    }

    fun rejectIncomingCall() {
        val session = _callSession.value ?: return
        discoveryService.sendCallSignal(
            session.peerIp,
            RichPacket(
                type = "CALL_REJECT",
                senderId = discoveryService.ownIpAddress.value ?: "",
                senderName = "",
                callId = session.callId
            )
        )
        _callSession.value = null
    }

    fun endCall(sendHangup: Boolean = true) {
        val session = _callSession.value
        if (session != null && sendHangup) {
            discoveryService.sendCallSignal(
                session.peerIp,
                RichPacket(
                    type = "CALL_HANGUP",
                    senderId = discoveryService.ownIpAddress.value ?: "",
                    senderName = "",
                    callId = session.callId
                )
            )
        }
        stopCallStreaming()
        _callSession.value = null
        _remoteVideoBitmap.value = null
    }

    private fun startCallTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000)
                val current = _callSession.value ?: break
                if (current.state == CallState.CONNECTED) {
                    _callSession.value = current.copy(durationSeconds = current.durationSeconds + 1)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startCallStreaming(peerIp: String, isVideo: Boolean) {
        try {
            // Audio Sockets & Engine
            audioSocket?.close()
            audioSocket = DatagramSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress(NetworkConstants.VOICE_CALL_PORT))
            }

            // Audio Record setup
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                channelConfigIn,
                audioFormat,
                bufferSizeAudioIn
            )

            if (AcousticEchoCanceler.isAvailable()) {
                echoCanceler = AcousticEchoCanceler.create(audioRecord!!.audioSessionId)?.apply {
                    enabled = true
                }
            }
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(audioRecord!!.audioSessionId)?.apply {
                    enabled = true
                }
            }

            // Audio Track setup
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfigOut)
                        .build()
                )
                .setBufferSizeInBytes(bufferSizeAudioOut)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioRecord?.startRecording()
            audioTrack?.play()

            // Audio send loop
            audioRecordJob = scope.launch {
                val targetAddr = InetAddress.getByName(peerIp)
                val buffer = ByteArray(bufferSizeAudioIn)
                while (isActive) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        val session = _callSession.value
                        if (session?.isMuted != true) {
                            val packet = DatagramPacket(buffer, read, targetAddr, NetworkConstants.VOICE_CALL_PORT)
                            audioSocket?.send(packet)
                        }
                    }
                }
            }

            // Audio receive loop
            audioPlayJob = scope.launch {
                val buffer = ByteArray(4096)
                while (isActive) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        audioSocket?.receive(packet)
                        if (packet.length > 0) {
                            audioTrack?.write(packet.data, 0, packet.length)
                        }
                    } catch (e: Exception) {
                        if (isActive) delay(50)
                    }
                }
            }

            // Video Engine
            if (isVideo) {
                videoSocket?.close()
                videoSocket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(NetworkConstants.VIDEO_CALL_PORT))
                }

                videoReceiveJob = scope.launch {
                    val buffer = ByteArray(65507)
                    while (isActive) {
                        try {
                            val packet = DatagramPacket(buffer, buffer.size)
                            videoSocket?.receive(packet)
                            if (packet.length > 0) {
                                val bmp = BitmapFactory.decodeByteArray(packet.data, 0, packet.length)
                                if (bmp != null) {
                                    _remoteVideoBitmap.value = bmp
                                }
                            }
                        } catch (e: Exception) {
                            if (isActive) delay(50)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting call streams: ${e.message}")
        }
    }

    fun processCameraFrame(imageProxy: ImageProxy) {
        val session = _callSession.value ?: run {
            imageProxy.close()
            return
        }
        if (!session.isVideo || !session.isCameraEnabled || session.state != CallState.CONNECTED) {
            imageProxy.close()
            return
        }

        scope.launch {
            try {
                val targetAddr = InetAddress.getByName(session.peerIp)
                val jpegBytes = imageProxyToJpeg(imageProxy)
                if (jpegBytes != null && jpegBytes.size < 65000) {
                    val packet = DatagramPacket(jpegBytes, jpegBytes.size, targetAddr, NetworkConstants.VIDEO_CALL_PORT)
                    videoSocket?.send(packet)
                }
            } catch (e: Exception) {
                // Ignore dropped frames
            } finally {
                imageProxy.close()
            }
        }
    }

    private fun imageProxyToJpeg(image: ImageProxy): ByteArray? {
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 40, out)
        return out.toByteArray()
    }

    fun toggleMute() {
        val current = _callSession.value ?: return
        _callSession.value = current.copy(isMuted = !current.isMuted)
    }

    fun toggleSpeaker() {
        val current = _callSession.value ?: return
        val newSpeaker = !current.isSpeakerOn
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.isSpeakerphoneOn = newSpeaker
        _callSession.value = current.copy(isSpeakerOn = newSpeaker)
    }

    fun toggleCamera() {
        val current = _callSession.value ?: return
        _callSession.value = current.copy(isCameraEnabled = !current.isCameraEnabled)
    }

    fun switchCamera() {
        val current = _callSession.value ?: return
        _callSession.value = current.copy(isFrontCamera = !current.isFrontCamera)
    }

    private fun stopCallStreaming() {
        timerJob?.cancel()
        audioRecordJob?.cancel()
        audioPlayJob?.cancel()
        videoReceiveJob?.cancel()

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {}
        audioRecord = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {}
        audioTrack = null

        try {
            echoCanceler?.release()
            noiseSuppressor?.release()
        } catch (e: Exception) {}

        try {
            audioSocket?.close()
            videoSocket?.close()
        } catch (e: Exception) {}
        audioSocket = null
        videoSocket = null
    }
}
