package com.zeronetwork.connectivity.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronetwork.connectivity.data.model.Peer
import com.zeronetwork.connectivity.ui.components.BottomDock
import com.zeronetwork.connectivity.ui.components.SignalTopBar
import com.zeronetwork.connectivity.ui.screens.findothers.FindOthersScreen
import com.zeronetwork.connectivity.ui.screens.group.CreateGroupDialog
import com.zeronetwork.connectivity.ui.screens.profile.ProfileScreen
import com.zeronetwork.connectivity.ui.theme.SignalBlue
import com.zeronetwork.connectivity.ui.viewmodel.BottomTab
import com.zeronetwork.connectivity.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit,
    onNavigateToUpdates: () -> Unit,
    onConversationClick: (String, String, Boolean) -> Unit,
    onStartVoiceCall: (Peer) -> Unit,
    onStartVideoCall: (Peer) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val archivedConversations by viewModel.archivedConversations.collectAsState()
    val onlinePeers by viewModel.onlinePeers.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val networkStats by viewModel.networkStats.collectAsState()
    val ownIp by viewModel.ownIpAddress.collectAsState()
    val isMenuOpen by viewModel.showMenuPopup.collectAsState()
    val showCreateGroup by viewModel.showCreateGroup.collectAsState()
    val showHomeBanner by viewModel.showHomeBanner.collectAsState()
    val latestRelease by viewModel.latestRelease.collectAsState()

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    LaunchedEffect(showHomeBanner) {
        if (showHomeBanner) {
            delay(2000)
            viewModel.dismissHomeBanner()
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (currentTab != BottomTab.PROFILE) {
            viewModel.setTab(if (pagerState.currentPage == 0) BottomTab.CHATS else BottomTab.FIND_OTHERS)
        }
    }

    LaunchedEffect(currentTab) {
        if (currentTab == BottomTab.CHATS && pagerState.currentPage != 0) {
            pagerState.animateScrollToPage(0)
        } else if (currentTab == BottomTab.FIND_OTHERS && pagerState.currentPage != 1) {
            pagerState.animateScrollToPage(1)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                SignalTopBar(
                    networkStats = networkStats,
                    isMenuOpen = isMenuOpen,
                    onMenuToggle = { viewModel.toggleMenuPopup() },
                    onOpenSettings = onOpenSettings,
                    onOpenCreateGroup = { viewModel.openCreateGroup() }
                )

                AnimatedVisibility(
                    visible = showHomeBanner && latestRelease != null,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable {
                                onNavigateToUpdates()
                                viewModel.dismissHomeBanner()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "🚀 New Update Available: ${latestRelease?.tagName ?: "v1.0.2"} — Tap to View",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                if (currentTab != BottomTab.PROFILE) {
                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = SignalBlue,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                color = SignalBlue,
                                height = 3.dp
                            )
                        }
                    ) {
                        Tab(
                            selected = pagerState.currentPage == 0,
                            onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                            text = {
                                Text(
                                    text = "Chats",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 15.sp,
                                        fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (pagerState.currentPage == 0) SignalBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )

                        Tab(
                            selected = pagerState.currentPage == 1,
                            onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                            text = {
                                Text(
                                    text = "Find Others (${onlinePeers.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 15.sp,
                                        fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (pagerState.currentPage == 1) SignalBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f)
                    ) { page ->
                        when (page) {
                            0 -> ChatsTab(
                                conversations = conversations,
                                archivedConversations = archivedConversations,
                                onlinePeers = onlinePeers,
                                onConversationClick = { conv -> onConversationClick(conv.id, conv.title, conv.isGroup) },
                                onPinToggle = { conv -> viewModel.togglePin(conv) },
                                onArchiveToggle = { conv -> viewModel.toggleArchive(conv) },
                                onMuteToggle = { conv -> viewModel.toggleMute(conv) },
                                onDeleteConversation = { conv -> viewModel.deleteConversation(conv) }
                            )
                            1 -> FindOthersScreen(
                                onlinePeers = onlinePeers,
                                ownIp = ownIp,
                                ownUsername = userProfile.username,
                                onPeerClick = { peer -> onConversationClick(peer.ipAddress, peer.username, false) },
                                onVoiceCallClick = onStartVoiceCall,
                                onVideoCallClick = onStartVideoCall,
                                onRefreshScan = { viewModel.triggerSubnetScan() },
                                onManualConnect = { ip -> viewModel.probePeer(ip) }
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.weight(1f)) {
                        ProfileScreen(
                            userProfile = userProfile,
                            onSaveProfile = { viewModel.saveUserProfile(it) },
                            onNavigateToUpdates = onNavigateToUpdates
                        )
                    }
                }
            }

            BottomDock(
                currentTab = currentTab,
                onTabSelected = { tab -> viewModel.setTab(tab) },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            if (showCreateGroup) {
                CreateGroupDialog(
                    availablePeers = onlinePeers,
                    onDismiss = { viewModel.closeCreateGroup() },
                    onCreateGroup = { name, memberIps -> viewModel.createGroup(name, memberIps) }
                )
            }
        }
    }
}
