package com.zeronetwork.connectivity.ui.screens.chat

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronetwork.connectivity.data.model.MessageType
import com.zeronetwork.connectivity.ui.components.MediaPickerSheet
import com.zeronetwork.connectivity.ui.components.SpringMessageBubble
import com.zeronetwork.connectivity.ui.components.TelegramVoiceRecorderButton
import com.zeronetwork.connectivity.ui.components.TypingWaveIndicator
import com.zeronetwork.connectivity.ui.theme.AvatarColors
import com.zeronetwork.connectivity.ui.theme.EmeraldGreen
import com.zeronetwork.connectivity.ui.theme.SignalBlue
import com.zeronetwork.connectivity.ui.viewmodel.ChatViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversationId: String,
    peerTitle: String,
    isGroup: Boolean,
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onStartVoiceCall: (String, String) -> Unit,
    onStartVideoCall: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messages by viewModel.currentMessages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val activePeer by viewModel.activePeer.collectAsState()

    var showMediaSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val name = getFileName(context, it)
            viewModel.sendMediaUri(it, name, MessageType.IMAGE)
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val name = getFileName(context, it)
            viewModel.sendMediaUri(it, name, MessageType.VIDEO)
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val name = getFileName(context, it)
            viewModel.sendMediaUri(it, name, MessageType.FILE)
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val name = getFileName(context, it)
            viewModel.sendMediaUri(it, name, MessageType.AUDIO)
        }
    }

    LaunchedEffect(conversationId) {
        viewModel.setConversation(conversationId, activePeer)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Chat Top Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val colorIdx = activePeer?.avatarColorIndex ?: (conversationId.hashCode().mod(6).let { if (it < 0) -it else it })
                    val avatarColor = AvatarColors[colorIdx % AvatarColors.size]

                    Surface(
                        shape = CircleShape,
                        color = avatarColor,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = peerTitle.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = peerTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isGroup) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (activePeer?.isOnline == true) EmeraldGreen else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = if (isGroup) "LAN Group" else if (activePeer?.isTyping == true) "typing..." else if (activePeer?.isOnline == true) "online on LAN" else "offline ($conversationId)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = if (activePeer?.isTyping == true) SignalBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!isGroup) {
                        IconButton(onClick = { onStartVoiceCall(conversationId, peerTitle) }) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Voice Call",
                                tint = SignalBlue
                            )
                        }

                        IconButton(onClick = { onStartVideoCall(conversationId, peerTitle) }) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = "Video Call",
                                tint = SignalBlue
                            )
                        }
                    }
                }
            }

            // Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    SpringMessageBubble(message = message)
                }

                if (activePeer?.isTyping == true) {
                    item {
                        TypingWaveIndicator(senderName = peerTitle)
                    }
                }
            }

            // Bottom Composer Bar with Telegram Voice Recorder
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showMediaSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Attach Media",
                            tint = SignalBlue,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SignalBlue.copy(alpha = 0.12f))
                                .padding(4.dp)
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.onTextChanged(it) },
                        placeholder = { Text("Message...", style = MaterialTheme.typography.bodyMedium) },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { viewModel.sendMessage() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    )

                    if (inputText.isNotBlank()) {
                        IconButton(
                            onClick = { viewModel.sendMessage() },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(SignalBlue)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        // Telegram-Style Hold-to-record Voice Button
                        TelegramVoiceRecorderButton(
                            onVoiceRecorded = { file, durationSec ->
                                viewModel.sendMediaUri(Uri.fromFile(file), file.name, MessageType.AUDIO)
                            }
                        )
                    }
                }
            }
        }

        // Media Picker Bottom Sheet
        if (showMediaSheet) {
            MediaPickerSheet(
                onDismiss = { showMediaSheet = false },
                onPickImage = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickVideo = {
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                onPickAudio = {
                    audioPickerLauncher.launch("audio/*")
                },
                onPickFile = {
                    documentPickerLauncher.launch("*/*")
                }
            )
        }
    }
}

private fun getFileName(context: android.content.Context, uri: Uri): String {
    var name = "file_${System.currentTimeMillis()}"
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
    } catch (e: Exception) {}
    return name
}
