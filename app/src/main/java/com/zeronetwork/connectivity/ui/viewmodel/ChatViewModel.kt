package com.zeronetwork.connectivity.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeronetwork.connectivity.ZeroNetworkApp
import com.zeronetwork.connectivity.data.model.ChatMessage
import com.zeronetwork.connectivity.data.model.MessageType
import com.zeronetwork.connectivity.data.model.Peer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class ChatViewModel : ViewModel() {
    private val app = ZeroNetworkApp.instance
    private val chatRepo = app.chatRepository
    private val peerRepo = app.peerRepository

    val activeConversationId = MutableStateFlow("")
    val activePeer = MutableStateFlow<Peer?>(null)
    val inputText = MutableStateFlow("")
    val isRecordingVoice = MutableStateFlow(false)
    val isPeerTyping = MutableStateFlow(false)
    val recordingDurationSec = MutableStateFlow(0)
    val audioAmplitudes = MutableStateFlow<List<Float>>(emptyList())

    private var recordingTimerJob: Job? = null
    private var typingJob: Job? = null
    private var messageFlowJob: Job? = null

    private val _messagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messagesFlow.asStateFlow()

    fun setConversation(conversationId: String, peer: Peer?) {
        activeConversationId.value = conversationId
        activePeer.value = peer

        messageFlowJob?.cancel()
        messageFlowJob = viewModelScope.launch {
            chatRepo.markConversationRead(conversationId)
            chatRepo.getMessagesForConversation(conversationId).collect { list ->
                _messagesFlow.value = list
            }
        }
    }

    fun setTyping(conversationId: String, isTyping: Boolean) {
        val peerIp = activePeer.value?.ipAddress ?: conversationId
        if (peerIp.isNotBlank() && !conversationId.startsWith("group_")) {
            peerRepo.sendTyping(peerIp, isTyping)
        }
    }

    fun sendMessage(conversationId: String, text: String) {
        val targetIp = activePeer.value?.ipAddress ?: conversationId
        viewModelScope.launch {
            chatRepo.sendTextMessage(conversationId, targetIp, text)
        }
    }

    fun sendFile(conversationId: String, file: File, type: MessageType) {
        val targetIp = activePeer.value?.ipAddress ?: conversationId
        viewModelScope.launch {
            chatRepo.sendFile(conversationId, targetIp, file, type)
        }
    }

    fun sendFileFromUri(conversationId: String, uri: Uri, fileName: String?, type: MessageType) {
        val targetIp = activePeer.value?.ipAddress ?: conversationId
        viewModelScope.launch {
            chatRepo.sendFileFromUri(conversationId, targetIp, uri, fileName, type)
        }
    }

    fun startVoiceRecording(): Boolean {
        val started = chatRepo.startVoiceRecording()
        if (started) {
            isRecordingVoice.value = true
            recordingDurationSec.value = 0
            audioAmplitudes.value = emptyList()

            recordingTimerJob?.cancel()
            recordingTimerJob = viewModelScope.launch {
                var seconds = 0
                val amps = mutableListOf<Float>()
                while (isActive && isRecordingVoice.value) {
                    delay(100)
                    val amp = (chatRepo.getMaxAmplitude() / 32767f).coerceIn(0.1f, 1.0f)
                    amps.add(amp)
                    if (amps.size > 30) amps.removeAt(0)
                    audioAmplitudes.value = amps.toList()

                    if (amps.size % 10 == 0) {
                        seconds++
                        recordingDurationSec.value = seconds
                    }
                }
            }
        }
        return started
    }

    fun getVoiceAmplitude(): Float {
        return (chatRepo.getMaxAmplitude() / 32767f).coerceIn(0.05f, 1.0f)
    }

    fun stopAndSendVoiceRecording(conversationId: String) {
        val targetIp = activePeer.value?.ipAddress ?: conversationId
        recordingTimerJob?.cancel()
        isRecordingVoice.value = false

        viewModelScope.launch {
            chatRepo.stopAndSendVoiceRecording(conversationId, targetIp)
        }
    }

    fun cancelVoiceRecording() {
        recordingTimerJob?.cancel()
        isRecordingVoice.value = false
        chatRepo.cancelVoiceRecording()
    }

    fun updateGroup(groupId: String, newTitle: String, newIps: String) {
        viewModelScope.launch {
            chatRepo.updateGroup(groupId, newTitle, newIps)
        }
    }

    fun clearHistory(conversationId: String) {
        viewModelScope.launch {
            chatRepo.clearHistory(conversationId)
        }
    }
}
