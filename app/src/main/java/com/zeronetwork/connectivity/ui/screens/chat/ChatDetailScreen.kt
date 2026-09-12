package com.zeronetwork.connectivity.ui.screens.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.zeronetwork.connectivity.data.model.MessageType
import com.zeronetwork.connectivity.ui.components.FullscreenImageViewer
import com.zeronetwork.connectivity.ui.components.MediaPickerSheet
import com.zeronetwork.connectivity.ui.components.SpringMessageBubble
import com.zeronetwork.connectivity.ui.components.TelegramVoiceRecorderButton
import com.zeronetwork.connectivity.ui.components.TypingWaveIndicator
import com.zeronetwork.connectivity.ui.screens.group.GroupSettingsDialog
import com.zeronetwork.connectivity.ui.viewmodel.ChatViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversationId: String,
    peerTitle: String,
    isGroup: Boolean = false,
    onBack: () -> Unit,
    onStartVoiceCall: (String, String) -> Unit,
    onStartVideoCall: (String, String) -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isPeerTyping by viewModel.isPeerTyping.collectAsState()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var showMediaPicker by remember { mutableStateOf(false) }
    var showGroupSettings by remember { mutableStateOf(false) }
    var showAvatarViewer by remember { mutableStateOf(false) }

    LaunchedEffect(conversationId) {
        viewModel.setConversation(conversationId, null)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendFileFromUri(conversationId, uri, null, MessageType.IMAGE)
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendFileFromUri(conversationId, uri, null, MessageType.VIDEO)
        }
    }

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendFileFromUri(conversationId, uri, null, MessageType.AUDIO)
        }
    }

    val docLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendFileFromUri(conversationId, uri, null, MessageType.FILE)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            if (isGroup) showGroupSettings = true
                            else showAvatarViewer = true
                        }
                    ) {
                        // Soft pastel avatar matching screenshot
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEADCF7),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isGroup) {
                                    Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF6B21A8))
                                } else {
                                    val initials = peerTitle.split(" ")
                                        .filter { it.isNotBlank() }
                                        .take(2)
                                        .map { it.first().uppercase() }
                                        .joinToString("")
                                        .ifEmpty { peerTitle.take(2).uppercase() }
                                    Text(
                                        text = initials,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6B21A8)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = peerTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isGroup) "Group • Tap for settings" else (if (isPeerTyping) "Typing..." else "Contact"),
                                fontSize = 12.sp,
                                color = if (isPeerTyping) MaterialTheme.colorScheme.primary else Color(0xFF64748B)
                            )
                        }
                    }
                },
                navigationIcon = {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(38.dp)
                            .clickable { onBack() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF1E293B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (!isGroup) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(38.dp)
                                .clickable { onStartVoiceCall(conversationId, peerTitle) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Voice Call",
                                    tint = Color(0xFF0F4C81),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = { showGroupSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Group Settings")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Exact Top Encryption Banner from Screenshot
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "This chat is end-to-end encrypted and stored only on these two devices.",
                                fontSize = 12.5.sp,
                                color = Color(0xFF475569),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    SpringMessageBubble(
                        message = msg,
                        onMediaClick = {
                            showAvatarViewer = true
                        }
                    )
                }
            }

            // Typing Indicator
            if (isPeerTyping) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TypingWaveIndicator()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$peerTitle is typing...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Exact Bottom Input Bar from Screenshot
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Circular paperclip button on left
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .size(42.dp)
                            .clickable { showMediaPicker = true }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach",
                                tint = Color(0xFF1E293B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Rounded pill text field "Type a message"
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            viewModel.setTyping(conversationId, it.isNotEmpty())
                        },
                        placeholder = {
                            Text("Type a message", fontSize = 14.sp, color = Color(0xFF94A3B8))
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFCBD5E1),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    )

                    // Right Button: If text entered, Send button; otherwise circular dark mic button!
                    if (inputText.isNotBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0F4C81),
                            modifier = Modifier
                                .size(44.dp)
                                .clickable {
                                    val toSend = inputText.trim()
                                    if (toSend.isNotEmpty()) {
                                        viewModel.sendMessage(conversationId, toSend)
                                        inputText = ""
                                        viewModel.setTyping(conversationId, false)
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        TelegramVoiceRecorderButton(
                            onVoiceRecorded = { voiceFile, durationSec ->
                                viewModel.sendFile(conversationId, voiceFile, MessageType.AUDIO)
                            }
                        )
                    }
                }
            }
        }
    }

    // Media Picker Sheet
    if (showMediaPicker) {
        MediaPickerSheet(
            onDismiss = { showMediaPicker = false },
            onPickImage = {
                showMediaPicker = false
                photoLauncher.launch("image/*")
            },
            onPickVideo = {
                showMediaPicker = false
                videoLauncher.launch("video/*")
            },
            onPickAudio = {
                showMediaPicker = false
                audioLauncher.launch("audio/*")
            },
            onPickFile = {
                showMediaPicker = false
                docLauncher.launch("*/*")
            }
        )
    }

    // Group Settings Dialog
    if (showGroupSettings) {
        GroupSettingsDialog(
            groupId = conversationId,
            currentTitle = peerTitle,
            participantIps = "",
            availablePeers = emptyList(),
            onDismiss = { showGroupSettings = false },
            onUpdateGroup = { newTitle, newIps -> viewModel.updateGroup(conversationId, newTitle, newIps) },
            onClearGroupChat = { viewModel.clearHistory(conversationId) },
            onLeaveGroup = {
                viewModel.clearHistory(conversationId)
                onBack()
            }
        )
    }

    // Fullscreen Avatar Lightbox
    if (showAvatarViewer) {
        FullscreenImageViewer(
            title = peerTitle,
            imageBase64 = null,
            avatarColorIndex = 0,
            onDismiss = { showAvatarViewer = false }
        )
    }
}
