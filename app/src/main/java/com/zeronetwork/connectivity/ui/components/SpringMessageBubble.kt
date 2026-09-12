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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.zeronetwork.connectivity.ui.theme.BubbleReceivedDark
import com.zeronetwork.connectivity.ui.theme.BubbleSentEmerald
import com.zeronetwork.connectivity.ui.theme.BubbleSentMidnight
import com.zeronetwork.connectivity.ui.theme.BubbleSentPurple
import com.zeronetwork.connectivity.ui.theme.BubbleSentSignal
import com.zeronetwork.connectivity.ui.theme.BubbleSentSunset
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

    // Telegram-style spring scale and translation animation
    val scale = remember { Animatable(0.75f) }
    val offsetY = remember { Animatable(30f) }

    LaunchedEffect(message.id) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    LaunchedEffect(message.id) {
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    val bubbleColor = if (isMine) {
        when (bubbleColorKey) {
            "EMERALD" -> BubbleSentEmerald
            "PURPLE" -> BubbleSentPurple
            "SUNSET" -> BubbleSentSunset
            "MIDNIGHT" -> BubbleSentMidnight
            else -> BubbleSentSignal
        }
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
    val timeColor = if (isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    val bubbleShape = if (isMine) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                translationY = offsetY.value
            },
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                if (!isMine && message.conversationId.startsWith("group_")) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                when (message.type) {
                    MessageType.TEXT -> {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    MessageType.IMAGE -> {
                        val file = message.filePath?.let { File(it) }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onMediaClick(message) }
                        ) {
                            AsyncImage(
                                model = file ?: message.filePath,
                                contentDescription = "Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                        if (message.content.isNotBlank() && message.content != message.fileName) {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                            )
                        }
                    }

                    MessageType.VIDEO -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { onMediaClick(message) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Video",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .padding(8.dp)
                            )
                        }
                        Text(
                            text = message.fileName ?: "Video message",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    MessageType.AUDIO -> {
                        AudioMessagePlayer(message = message, isMine = isMine)
                    }

                    MessageType.FILE -> {
                        FileMessageCard(message = message, isMine = isMine)
                    }

                    else -> {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }
                }

                // Timestamp & Delivery status
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val timeFormatted = remember(message.timestamp) {
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
                    }
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = timeColor
                    )
                    if (isMine) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (message.status) {
                            MessageStatus.SENDING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 1.5.dp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            MessageStatus.SENT -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Sent",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            MessageStatus.DELIVERED -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Delivered",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            MessageStatus.FAILED -> {
                                Text("!", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AudioMessagePlayer(message: ChatMessage, isMine: Boolean) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    val mediaPlayer = remember { MediaPlayer() }

    DisposableEffect(message.filePath) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            } catch (e: Exception) {}
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (isMine) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(38.dp)
                .clickable {
                    val path = message.filePath
                    if (path != null && File(path).exists()) {
                        if (isPlaying) {
                            mediaPlayer.pause()
                            isPlaying = false
                        } else {
                            try {
                                mediaPlayer.reset()
                                mediaPlayer.setDataSource(path)
                                mediaPlayer.prepare()
                                mediaPlayer.start()
                                isPlaying = true
                                mediaPlayer.setOnCompletionListener {
                                    isPlaying = false
                                    currentProgress = 0f
                                }
                            } catch (e: Exception) {}
                        }
                    }
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "Waveform",
                tint = if (isMine) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
            )
            Text(
                text = "Voice Note • ${message.fileName ?: ""}",
                style = MaterialTheme.typography.labelSmall,
                color = if (isMine) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FileMessageCard(message: ChatMessage, isMine: Boolean) {
    val context = LocalContext.current
    val fileSizeFormatted = remember(message.fileSize) {
        val kb = message.fileSize / 1024f
        if (kb > 1024) String.format("%.1f MB", kb / 1024f) else String.format("%.0f KB", kb)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable {
                message.filePath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        try {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "*/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Open file"))
                        } catch (e: Exception) {}
                    }
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isMine) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "File",
                    tint = if (isMine) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.fileName ?: "Document",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = fileSizeFormatted,
                style = MaterialTheme.typography.labelSmall,
                color = if (isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
