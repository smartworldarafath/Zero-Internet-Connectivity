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
    val recordingDurationSec = MutableStateFlow(0)
    val audioAmplitudes = MutableStateFlow<List<Float>>(emptyList())

    private var recordingTimerJob: Job? = null
    private var typingJob: Job? = null

    val messages: StateFlow<List<ChatMessage>> = MutableStateFlow<List<ChatMessage>>(emptyList())
    private var messageFlowJob: Job? = null
    private val _messagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    val currentMessages: StateFlow<List<ChatMessage>> = _messagesFlow.asStateFlow()

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

    fun onTextChanged(newText: String) {
        inputText.value = newText
        val peerIp = activePeer.value?.ipAddress ?: activeConversationId.value
        if (peerIp.isNotEmpty() && !activeConversationId.value.startsWith("group_")) {
            peerRepo.sendTyping(peerIp, newText.isNotEmpty())
            typingJob?.cancel()
            typingJob = viewModelScope.launch {
                delay(3000)
                peerRepo.sendTyping(peerIp, false)
            }
        }
    }

    fun sendMessage() {
        val text = inputText.value.trim()
        if (text.isEmpty()) return
        val convId = activeConversationId.value
        val targetIp = activePeer.value?.ipAddress ?: convId
        inputText.value = ""

        viewModelScope.launch {
            peerRepo.sendTyping(targetIp, false)
            chatRepo.sendTextMessage(convId, targetIp, text)
        }
    }

    fun sendMediaUri(uri: Uri, fileName: String?, type: MessageType) {
        val convId = activeConversationId.value
        val targetIp = activePeer.value?.ipAddress ?: convId
        viewModelScope.launch {
            chatRepo.sendFileFromUri(convId, targetIp, uri, fileName, type)
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

    fun stopAndSendVoiceRecording() {
        val convId = activeConversationId.value
        val targetIp = activePeer.value?.ipAddress ?: convId
        recordingTimerJob?.cancel()
        isRecordingVoice.value = false

        viewModelScope.launch {
            chatRepo.stopAndSendVoiceRecording(convId, targetIp)
        }
    }

    fun cancelVoiceRecording() {
        recordingTimerJob?.cancel()
        isRecordingVoice.value = false
        chatRepo.cancelVoiceRecording()
    }
}
