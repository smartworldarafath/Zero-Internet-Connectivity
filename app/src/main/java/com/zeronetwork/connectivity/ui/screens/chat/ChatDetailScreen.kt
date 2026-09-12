package com.zeronetwork.connectivity.ui.screens.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeronetwork.connectivity.data.model.MessageType
import com.zeronetwork.connectivity.ui.components.FullscreenImageViewer
import com.zeronetwork.connectivity.ui.components.MediaPickerSheet
import com.zeronetwork.connectivity.ui.components.SpringMessageBubble
import com.zeronetwork.connectivity.ui.components.TelegramVoiceRecorderButton
import com.zeronetwork.connectivity.ui.screens.group.GroupSettingsDialog
import com.zeronetwork.connectivity.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversationId: String,
    peerTitle: String,
    isGroup: Boolean,
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onStartVoiceCall: (String, String) -> Unit,
    onStartVideoCall: (String, String) -> Unit
) {
    val listState = rememberLazyListState()
    val messages by viewModel.messages.collectAsState()
    val isPeerTyping by viewModel.isPeerTyping.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showMediaPicker by remember { mutableStateOf(false) }
    var showGroupSettings by remember { mutableStateOf(false) }
    var showAvatarViewer by remember { mutableStateOf(false) }

    LaunchedEffect(conversationId) {
        viewModel.setConversation(conversationId, null)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
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
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isGroup) {
                                    Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Text(
                                        text = peerTitle.take(2).uppercase(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = peerTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isGroup) "Group • Tap for info" else (if (isPeerTyping) "Typing..." else "Connected on LAN"),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isPeerTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isGroup) {
                        IconButton(onClick = { onStartVoiceCall(conversationId, peerTitle) }) {
                            Icon(Icons.Default.Call, contentDescription = "Voice Call")
                        }
                        IconButton(onClick = { onStartVideoCall(conversationId, peerTitle) }) {
                            Icon(Icons.Default.Videocam, contentDescription = "Video Call")
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    SpringMessageBubble(
                        message = msg,
                        onMediaClick = {
                            showAvatarViewer = true
                        }
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    IconButton(onClick = { showMediaPicker = true }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach")
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            viewModel.setTyping(conversationId, it.isNotBlank())
                        },
                        placeholder = { Text("Type an offline message...") },
                        singleLine = false,
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    )

                    if (inputText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val text = inputText.trim()
                                if (text.isNotBlank()) {
                                    viewModel.sendMessage(conversationId, text)
                                    inputText = ""
                                    viewModel.setTyping(conversationId, false)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        TelegramVoiceRecorderButton(
                            onVoiceRecorded = { file, duration ->
                                viewModel.sendFile(conversationId, file, MessageType.AUDIO)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showMediaPicker) {
        MediaPickerSheet(
            onDismiss = { showMediaPicker = false },
            onPickImage = { photoLauncher.launch("image/*") },
            onPickVideo = { videoLauncher.launch("video/*") },
            onPickAudio = { audioLauncher.launch("audio/*") },
            onPickFile = { docLauncher.launch("*/*") }
        )
    }

    if (showGroupSettings) {
        GroupSettingsDialog(
            groupId = conversationId,
            currentTitle = peerTitle,
            participantIps = "",
            availablePeers = emptyList(),
            onDismiss = { showGroupSettings = false },
            onUpdateGroup = { newTitle, newIps ->
                viewModel.updateGroup(conversationId, newTitle, newIps)
            },
            onClearGroupChat = { viewModel.clearHistory(conversationId) },
            onLeaveGroup = {
                viewModel.clearHistory(conversationId)
                onBack()
            }
        )
    }

    if (showAvatarViewer) {
        FullscreenImageViewer(
            title = peerTitle,
            onDismiss = { showAvatarViewer = false }
        )
    }
}
