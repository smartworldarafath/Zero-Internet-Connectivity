package com.zeronetwork.connectivity.data.repository

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import com.zeronetwork.connectivity.data.local.ConversationDao
import com.zeronetwork.connectivity.data.local.MessageDao
import com.zeronetwork.connectivity.data.local.PeerDao
import com.zeronetwork.connectivity.data.local.PreferencesManager
import com.zeronetwork.connectivity.data.model.ChatConversation
import com.zeronetwork.connectivity.data.model.ChatMessage
import com.zeronetwork.connectivity.data.model.MessageStatus
import com.zeronetwork.connectivity.data.model.MessageType
import com.zeronetwork.connectivity.network.FileTransferEvent
import com.zeronetwork.connectivity.network.LanDiscoveryService
import com.zeronetwork.connectivity.network.LanEvent
import com.zeronetwork.connectivity.network.LanFileTransferService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class ChatRepository(
    private val context: Context,
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val peerDao: PeerDao,
    private val discoveryService: LanDiscoveryService,
    private val fileTransferService: LanFileTransferService,
    private val preferencesManager: PreferencesManager
) {
    private val TAG = "ChatRepository"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val conversations: Flow<List<ChatConversation>> = conversationDao.getActiveConversations()
    val archivedConversations: Flow<List<ChatConversation>> = conversationDao.getArchivedConversations()

    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var recordingStartTime: Long = 0L

    init {
        listenToIncomingMessages()
        listenToFileTransfers()
        listenToGroupUpdates()
    }

    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForConversation(conversationId)
    }

    private fun listenToGroupUpdates() {
        scope.launch {
            discoveryService.events.collect { event ->
                if (event is LanEvent.GroupUpdateEvent) {
                    val existing = conversationDao.getConversationById(event.groupId)
                    if (existing != null) {
                        conversationDao.updateGroupTitleAndMembers(event.groupId, event.groupTitle, event.memberIps)
                    } else {
                        val group = ChatConversation(
                            id = event.groupId,
                            title = event.groupTitle,
                            isGroup = true,
                            participantIps = event.memberIps,
                            lastMessageSnippet = "Group updated",
                            lastMessageTimestamp = System.currentTimeMillis()
                        )
                        conversationDao.insertOrUpdateConversation(group)
                    }
                }
            }
        }
    }

    private fun listenToIncomingMessages() {
        scope.launch {
            discoveryService.events.collect { event ->
                when (event) {
                    is LanEvent.TextMessage -> {
                        val senderPeer = peerDao.getPeerByIp(event.senderIp)
                        val senderName = senderPeer?.username ?: event.senderIp

                        val message = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            conversationId = event.senderIp,
                            senderId = event.senderIp,
                            senderName = senderName,
                            senderAddress = event.senderIp,
                            type = MessageType.TEXT,
                            content = event.text,
                            timestamp = event.timestamp,
                            isMine = false,
                            status = MessageStatus.DELIVERED
                        )

                        messageDao.insertMessage(message)
                        updateConversationSnippet(
                            conversationId = event.senderIp,
                            title = senderName,
                            lastSnippet = event.text,
                            type = MessageType.TEXT,
                            incrementUnread = true
                        )
                    }
                    is LanEvent.FileCompleted -> {
                        val senderPeer = peerDao.getPeerByIp(event.senderIp)
                        val senderName = senderPeer?.username ?: event.senderIp
                        val ext = event.fileName.substringAfterLast('.', "").lowercase()
                        val type = when (ext) {
                            "jpg", "jpeg", "png", "webp", "gif" -> MessageType.IMAGE
                            "mp4", "mkv", "mov", "webm", "avi" -> MessageType.VIDEO
                            "m4a", "mp3", "aac", "wav", "ogg" -> MessageType.AUDIO
                            else -> MessageType.FILE
                        }

                        val message = ChatMessage(
                            id = UUID.randomUUID().toString(),
                            conversationId = event.senderIp,
                            senderId = event.senderIp,
                            senderName = senderName,
                            senderAddress = event.senderIp,
                            type = type,
                            content = event.fileName,
                            filePath = event.filePath,
                            fileName = event.fileName,
                            fileSize = event.fileSize,
                            timestamp = System.currentTimeMillis(),
                            isMine = false,
                            status = MessageStatus.DELIVERED,
                            transferProgress = 1.0f
                        )

                        messageDao.insertMessage(message)
                        updateConversationSnippet(
                            conversationId = event.senderIp,
                            title = senderName,
                            lastSnippet = "[${type.name.lowercase().replaceFirstChar { it.uppercase() }}] ${event.fileName}",
                            type = type,
                            incrementUnread = true
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    private fun listenToFileTransfers() {
        scope.launch {
            fileTransferService.transferEvents.collect { event ->
                when (event) {
                    is FileTransferEvent.Completed -> {
                        val senderPeer = peerDao.getPeerByIp(event.senderIp)
                        val senderName = senderPeer?.username ?: event.senderIp

                        val message = ChatMessage(
                            id = event.transferId,
                            conversationId = event.senderIp,
                            senderId = event.senderIp,
                            senderName = senderName,
                            senderAddress = event.senderIp,
                            type = event.type,
                            content = event.fileName,
                            filePath = event.filePath,
                            fileName = event.fileName,
                            fileSize = event.fileSize,
                            timestamp = System.currentTimeMillis(),
                            isMine = false,
                            status = MessageStatus.DELIVERED,
                            transferProgress = 1.0f
                        )
                        messageDao.insertMessage(message)
                        updateConversationSnippet(
                            conversationId = event.senderIp,
                            title = senderName,
                            lastSnippet = "[${event.type.name.lowercase().replaceFirstChar { it.uppercase() }}] ${event.fileName}",
                            type = event.type,
                            incrementUnread = true
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    suspend fun sendTextMessage(conversationId: String, targetIp: String, text: String) {
        val myName = preferencesManager.getUsername()
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = "me",
            senderName = myName,
            senderAddress = discoveryService.ownIpAddress.value ?: "127.0.0.1",
            type = MessageType.TEXT,
            content = text,
            timestamp = System.currentTimeMillis(),
            isMine = true,
            status = MessageStatus.SENT
        )

        messageDao.insertMessage(message)

        if (conversationId.startsWith("group_")) {
            val conv = conversationDao.getConversationById(conversationId)
            val ips = conv?.participantIps?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            for (ip in ips) {
                discoveryService.sendDirectText(ip.trim(), text)
            }
            updateConversationSnippet(conversationId, conv?.title ?: "Group", text, MessageType.TEXT, incrementUnread = false)
        } else {
            discoveryService.sendDirectText(targetIp, text)
            val peer = peerDao.getPeerByIp(targetIp)
            val title = peer?.username ?: targetIp
            updateConversationSnippet(conversationId, title, text, MessageType.TEXT, incrementUnread = false)
        }
    }

    suspend fun sendFile(conversationId: String, targetIp: String, file: File, type: MessageType) {
        val myName = preferencesManager.getUsername()
        val transferId = UUID.randomUUID().toString()

        val message = ChatMessage(
            id = transferId,
            conversationId = conversationId,
            senderId = "me",
            senderName = myName,
            senderAddress = discoveryService.ownIpAddress.value ?: "127.0.0.1",
            type = type,
            content = file.name,
            filePath = file.absolutePath,
            fileName = file.name,
            fileSize = file.length(),
            timestamp = System.currentTimeMillis(),
            isMine = true,
            status = MessageStatus.SENDING,
            transferProgress = 0f
        )
        messageDao.insertMessage(message)

        val peer = peerDao.getPeerByIp(targetIp)
        val title = peer?.username ?: targetIp
        updateConversationSnippet(conversationId, title, "[${type.name}] ${file.name}", type, incrementUnread = false)

        scope.launch {
            val success = fileTransferService.sendFile(targetIp, transferId, file, type)
            val updatedStatus = if (success) MessageStatus.DELIVERED else MessageStatus.FAILED
            messageDao.updateMessage(message.copy(status = updatedStatus, transferProgress = 1.0f))
        }
    }

    suspend fun sendFileFromUri(conversationId: String, targetIp: String, uri: Uri, originalName: String?, type: MessageType) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val fileName = originalName ?: "file_${System.currentTimeMillis()}"
                val tempFile = File(context.cacheDir, fileName)
                FileOutputStream(tempFile).use { out ->
                    inputStream.copyTo(out)
                }
                sendFile(conversationId, targetIp, tempFile, type)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying URI to temp file: ${e.message}")
        }
    }

    fun startVoiceRecording(): Boolean {
        return try {
            val audioDir = File(context.filesDir, "voice_notes").apply { mkdirs() }
            currentRecordingFile = File(audioDir, "audio_${System.currentTimeMillis()}.m4a")
            recordingStartTime = System.currentTimeMillis()

            mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(currentRecordingFile!!.absolutePath)
                prepare()
                start()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice recording: ${e.message}")
            false
        }
    }

    fun getMaxAmplitude(): Int {
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
    }

    suspend fun stopAndSendVoiceRecording(conversationId: String, targetIp: String): Boolean {
        return try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null

            val file = currentRecordingFile
            if (file != null && file.exists() && file.length() > 0) {
                val duration = System.currentTimeMillis() - recordingStartTime
                if (duration > 800) {
                    sendFile(conversationId, targetIp, file, MessageType.AUDIO)
                    true
                } else {
                    file.delete()
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping voice recording: ${e.message}")
            false
        }
    }

    fun cancelVoiceRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            currentRecordingFile?.delete()
            currentRecordingFile = null
        } catch (e: Exception) {}
    }

    private suspend fun updateConversationSnippet(
        conversationId: String,
        title: String,
        lastSnippet: String,
        type: MessageType,
        incrementUnread: Boolean
    ) {
        val existing = conversationDao.getConversationById(conversationId)
        val unread = if (incrementUnread) (existing?.unreadCount ?: 0) + 1 else (existing?.unreadCount ?: 0)
        val colorIdx = existing?.avatarColorIndex ?: (conversationId.hashCode().mod(6).let { if (it < 0) -it else it })

        val updated = ChatConversation(
            id = conversationId,
            title = title,
            isGroup = existing?.isGroup ?: conversationId.startsWith("group_"),
            participantIps = existing?.participantIps ?: "",
            lastMessageSnippet = lastSnippet,
            lastMessageType = type,
            lastMessageTimestamp = System.currentTimeMillis(),
            unreadCount = unread,
            avatarColorIndex = colorIdx,
            isArchived = existing?.isArchived ?: false,
            isPinned = existing?.isPinned ?: false,
            isMuted = existing?.isMuted ?: false,
            adminIp = existing?.adminIp
        )
        conversationDao.insertOrUpdateConversation(updated)
    }

    suspend fun setArchived(id: String, isArchived: Boolean) {
        conversationDao.setArchived(id, isArchived)
    }

    suspend fun setPinned(id: String, isPinned: Boolean) {
        conversationDao.setPinned(id, isPinned)
    }

    suspend fun setMuted(id: String, isMuted: Boolean) {
        conversationDao.setMuted(id, isMuted)
    }

    suspend fun updateGroup(groupId: String, newTitle: String, newMemberIps: String) {
        conversationDao.updateGroupTitleAndMembers(groupId, newTitle, newMemberIps)
        discoveryService.sendGroupUpdate(groupId, newTitle, newMemberIps)
    }

    suspend fun markConversationRead(conversationId: String) {
        conversationDao.markConversationAsRead(conversationId)
    }

    suspend fun clearHistory(conversationId: String) {
        messageDao.deleteMessagesForConversation(conversationId)
        conversationDao.deleteConversation(conversationId)
    }

    suspend fun clearAllHistory() {
        messageDao.clearAllMessages()
        conversationDao.clearAllConversations()
    }
}
