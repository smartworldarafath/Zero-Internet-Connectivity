package com.zeronetwork.connectivity.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeronetwork.connectivity.ZeroNetworkApp
import com.zeronetwork.connectivity.data.model.ChatConversation
import com.zeronetwork.connectivity.data.model.NetworkStats
import com.zeronetwork.connectivity.data.model.Peer
import com.zeronetwork.connectivity.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BottomTab {
    CHATS,
    FIND_OTHERS,
    PROFILE
}

class MainViewModel : ViewModel() {
    private val app = ZeroNetworkApp.instance
    private val chatRepo = app.chatRepository
    private val peerRepo = app.peerRepository
    private val prefs = app.preferencesManager
    private val networkMonitor = app.networkMonitor
    private val discoveryService = app.discoveryService
    private val updateManager = app.updateManager

    val currentTab = MutableStateFlow(BottomTab.CHATS)
    val searchQuery = MutableStateFlow("")

    val conversations: StateFlow<List<ChatConversation>> = chatRepo.conversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedConversations: StateFlow<List<ChatConversation>> = chatRepo.archivedConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val onlinePeers: StateFlow<List<Peer>> = peerRepo.onlinePeers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPeers: StateFlow<List<Peer>> = peerRepo.allPeers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile> = prefs.userProfile
    val networkStats: StateFlow<NetworkStats> = networkMonitor.networkStats
    val ownIpAddress: StateFlow<String?> = discoveryService.ownIpAddress

    val showHomeBanner: StateFlow<Boolean> = updateManager.showHomeBanner
    val latestRelease = updateManager.latestRelease

    val showMenuPopup = MutableStateFlow(false)
    val showCreateGroup = MutableStateFlow(false)

    fun setTab(tab: BottomTab) {
        currentTab.value = tab
    }

    fun toggleMenuPopup() {
        showMenuPopup.value = !showMenuPopup.value
    }

    fun openCreateGroup() {
        showMenuPopup.value = false
        showCreateGroup.value = true
    }

    fun closeCreateGroup() {
        showCreateGroup.value = false
    }

    fun dismissHomeBanner() {
        updateManager.dismissHomeBanner()
    }

    fun triggerSubnetScan() {
        discoveryService.triggerSubnetScan()
    }

    fun probePeer(ip: String) {
        discoveryService.probePeer(ip)
    }

    fun saveUserProfile(profile: UserProfile) {
        prefs.updateProfile(profile)
        discoveryService.updateUsername(profile.username)
        discoveryService.updateAvatar(profile.avatarUri)
    }

    fun togglePin(conversation: ChatConversation) {
        viewModelScope.launch {
            chatRepo.setPinned(conversation.id, !conversation.isPinned)
        }
    }

    fun toggleArchive(conversation: ChatConversation) {
        viewModelScope.launch {
            chatRepo.setArchived(conversation.id, !conversation.isArchived)
        }
    }

    fun toggleMute(conversation: ChatConversation) {
        viewModelScope.launch {
            chatRepo.setMuted(conversation.id, !conversation.isMuted)
        }
    }

    fun deleteConversation(conversation: ChatConversation) {
        viewModelScope.launch {
            chatRepo.clearHistory(conversation.id)
        }
    }

    fun createGroup(groupName: String, selectedMemberIps: List<String>) {
        if (groupName.isBlank() || selectedMemberIps.isEmpty()) return
        viewModelScope.launch {
            val ips = selectedMemberIps.joinToString(",")
            val groupId = "group_${System.currentTimeMillis()}"
            val conv = ChatConversation(
                id = groupId,
                title = groupName,
                isGroup = true,
                participantIps = ips,
                lastMessageSnippet = "Group created with ${selectedMemberIps.size} members",
                lastMessageTimestamp = System.currentTimeMillis(),
                adminIp = discoveryService.ownIpAddress.value
            )
            app.database.conversationDao().insertOrUpdateConversation(conv)
            discoveryService.sendGroupUpdate(groupId, groupName, ips)
            showCreateGroup.value = false
        }
    }
}
