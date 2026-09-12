package com.zeronetwork.connectivity.ui.components

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.zeronetwork.connectivity.data.model.ChatMessage
import com.zeronetwork.connectivity.data.model.MessageStatus
import com.zeronetwork.connectivity.data.model.MessageType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SpringMessageBubble(
    message: ChatMessage,
    bubbleColorKey: String = "SIGNAL_BLUE",
    onMediaClick: (ChatMessage) -> Unit = {}
) {
    val context = LocalContext.current
    val isMine = message.isMine

    // Spring animation on message entry
    val scale = remember { Animatable(0.85f) }
    val offsetY = remember { Animatable(20f) }

    LaunchedEffect(message.id) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }

    LaunchedEffect(message.id) {
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
        )
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = remember(message.timestamp) { timeFormatter.format(Date(message.timestamp)) }

    val isMissedCall = message.type == MessageType.CALL_LOG || message.content.contains("Missed call", ignoreCase = true)

    if (isMissedCall) {
        // Exact Missed Call Card from Screenshot
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    translationY = offsetY.value
                },
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 1.5.dp,
                modifier = Modifier.widthIn(min = 180.dp, max = 240.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFFDE8E8), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneMissed,
                                contentDescription = null,
                                tint = Color(0xFFE02424),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "Missed call",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = timeString,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
        return
    }

    // Exact Bubble Colors from Screenshot:
    // Sent (Mine): Soft Sky Blue #D9EBF7
    // Received (Theirs): Crisp Clean White #FFFFFF with subtle border/shadow
    val bubbleColor = if (isMine) Color(0xFFD9EBF7) else Color.White
    val textColor = Color(0xFF1E293B)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                translationY = offsetY.value
            },
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMine) 18.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 18.dp
            ),
            color = bubbleColor,
            shadowElevation = if (isMine) 0.5.dp else 1.5.dp,
            modifier = Modifier.widthIn(min = 80.dp, max = 310.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                when (message.type) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.content,
                            fontSize = 14.5.sp,
                            color = textColor,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.align(Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isMine) "$timeString · Read" else timeString,
                                fontSize = 11.sp,
                                color = if (isMine) Color(0xFF475569) else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    MessageType.AUDIO -> {
                        // Exact Voice Note Bubble from Screenshot
                        var isPlaying by remember { mutableStateOf(false) }
                        var playbackProgress by remember { mutableFloatStateOf(0f) }
                        var currentSec by remember { mutableStateOf("0:00") }
                        var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

                        val durationFormatted = remember(message.durationMs) {
                            val sec = if (message.durationMs > 0) message.durationMs / 1000 else 12L
                            val m = sec / 60
                            val s = sec % 60
                            String.format(Locale.getDefault(), "%d:%02d", m, s)
                        }

                        DisposableEffect(Unit) {
                            onDispose {
                                mediaPlayer?.release()
                                mediaPlayer = null
                            }
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Circular dark blue play/pause button
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(0xFF0F4C81), CircleShape)
                                        .clickable {
                                            if (isPlaying) {
                                                mediaPlayer?.pause()
                                                isPlaying = false
                                            } else {
                                                try {
                                                    val path = message.filePath
                                                    if (path != null && File(path).exists()) {
                                                        mediaPlayer?.release()
                                                        mediaPlayer = MediaPlayer().apply {
                                                            setDataSource(path)
                                                            prepare()
                                                            start()
                                                            setOnCompletionListener {
                                                                isPlaying = false
                                                                playbackProgress = 0f
                                                                currentSec = "0:00"
                                                            }
                                                        }
                                                        isPlaying = true
                                                    }
                                                } catch (e: Exception) {}
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Waveform Bars matching screenshot
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val barHeights = listOf(
                                        8, 12, 16, 10, 14, 18, 12, 16, 20, 14, 18, 12,
                                        16, 22, 14, 18, 12, 16, 10, 14, 18, 12, 16, 8
                                    )
                                    barHeights.forEach { h ->
                                        Box(
                                            modifier = Modifier
                                                .width(2.5.dp)
                                                .height(h.dp)
                                                .background(
                                                    if (isMine) Color(0xFF0F4C81).copy(alpha = 0.75f) else Color(0xFFCBD5E1),
                                                    RoundedCornerShape(1.dp)
                                                )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Timers row: 0:00 on left, 0:12 on right
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 48.dp, end = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = currentSec,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = durationFormatted,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = timeString,
                                fontSize = 10.5.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    MessageType.IMAGE -> {
                        val file = message.filePath?.let { File(it) }
                        if (file != null && file.exists()) {
                            AsyncImage(
                                model = file,
                                contentDescription = "Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onMediaClick(message) }
                            )
                        } else {
                            Text("[Photo]", fontSize = 14.sp, color = textColor)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isMine) "$timeString · Read" else timeString,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }

                    MessageType.VIDEO -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF0F4C81))
                            Text(message.fileName ?: "Video", fontSize = 14.sp, color = textColor)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isMine) "$timeString · Read" else timeString,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }

                    else -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF0F4C81))
                            Text(message.fileName ?: message.content, fontSize = 14.sp, color = textColor)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isMine) "$timeString · Read" else timeString,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }
    }
}
