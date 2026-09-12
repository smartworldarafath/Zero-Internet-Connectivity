package com.zeronetwork.connectivity.ui.components

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.roundToInt

class AudioRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    @Suppress("DEPRECATION")
    fun startRecording(): File? {
        return try {
            val outputDir = File(context.cacheDir, "audio_notes").apply { mkdirs() }
            val file = File(outputDir, "voice_${System.currentTimeMillis()}.m4a")
            currentFile = file

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(64000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            file
        } catch (e: Exception) {
            Log.e("AudioRecorderHelper", "Failed to start recorder: ${e.message}")
            null
        }
    }

    fun getMaxAmplitude(): Int {
        return try {
            recorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun stopRecording(): File? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            currentFile
        } catch (e: Exception) {
            recorder = null
            currentFile?.delete()
            null
        }
    }

    fun cancelRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {}
        recorder = null
        currentFile?.delete()
        currentFile = null
    }
}

@Composable
fun TelegramVoiceRecorderButton(
    modifier: Modifier = Modifier,
    onVoiceRecorded: (file: File, durationSec: Int) -> Unit
) {
    val context = LocalContext.current
    val recorderHelper = remember { AudioRecorderHelper(context) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableIntStateOf(0) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var isCancelled by remember { mutableStateOf(false) }

    val amplitudes = remember { mutableStateListOf<Float>() }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    fun vibrateShort() {
        try {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v?.vibrate(40)
            }
        } catch (e: Exception) {}
    }

    // Duration & amplitude ticker
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDuration = 0
            amplitudes.clear()
            while (isRecording) {
                delay(100)
                recordingDuration += 1
                val amp = (recorderHelper.getMaxAmplitude() / 32767f).coerceIn(0.1f, 1f)
                if (amplitudes.size > 20) amplitudes.removeAt(0)
                amplitudes.add(amp)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) {
                recorderHelper.cancelRecording()
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd
    ) {
        if (isRecording) {
            // Recording Active Bar overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Red recording dot & duration
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val sec = (recordingDuration / 10) % 60
                    val min = (recordingDuration / 10) / 60
                    Text(
                        text = String.format("%02d:%02d", min, sec),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Live Audio Waveform Bars
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    amplitudes.takeLast(14).forEach { amp ->
                        val barHeight = (28f * amp).dp.coerceIn(4.dp, 28.dp)
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                // Slide to cancel hint
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.offset { IntOffset(dragOffsetX.roundToInt().coerceAtMost(0), 0) }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Slide to cancel",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Hold-to-record microphone button
        Box(
            modifier = Modifier
                .offset { IntOffset(dragOffsetX.roundToInt().coerceAtMost(0), 0) }
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isRecording) Color(0xFFE53935) else MaterialTheme.colorScheme.primary)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isRecording = true
                            isCancelled = false
                            dragOffsetX = 0f
                            vibrateShort()
                            recorderHelper.startRecording()
                        },
                        onDragEnd = {
                            if (isRecording) {
                                isRecording = false
                                if (!isCancelled && dragOffsetX > -150f && recordingDuration > 5) {
                                    val file = recorderHelper.stopRecording()
                                    if (file != null && file.exists()) {
                                        vibrateShort()
                                        onVoiceRecorded(file, recordingDuration / 10)
                                    }
                                } else {
                                    recorderHelper.cancelRecording()
                                }
                                dragOffsetX = 0f
                            }
                        },
                        onDragCancel = {
                            if (isRecording) {
                                isRecording = false
                                recorderHelper.cancelRecording()
                                dragOffsetX = 0f
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetX = (dragOffsetX + dragAmount.x).coerceAtMost(0f)
                            if (dragOffsetX < -180f) {
                                isCancelled = true
                                isRecording = false
                                vibrateShort()
                                recorderHelper.cancelRecording()
                                dragOffsetX = 0f
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCancelled) Icons.Default.Delete else Icons.Default.Mic,
                contentDescription = "Hold to record voice",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .scale(if (isRecording) pulseScale else 1f)
            )
        }
    }
}
