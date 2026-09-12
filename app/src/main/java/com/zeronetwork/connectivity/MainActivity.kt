package com.zeronetwork.connectivity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.zeronetwork.connectivity.ui.screens.call.VideoCallScreen
import com.zeronetwork.connectivity.ui.screens.call.VoiceCallScreen
import com.zeronetwork.connectivity.ui.screens.chat.ChatDetailScreen
import com.zeronetwork.connectivity.ui.screens.main.MainScreen
import com.zeronetwork.connectivity.ui.screens.onboarding.UsernameSetupDialog
import com.zeronetwork.connectivity.ui.screens.permissions.PermissionRequestScreen
import com.zeronetwork.connectivity.ui.screens.settings.SettingsScreen
import com.zeronetwork.connectivity.ui.screens.splash.SplashScreen
import com.zeronetwork.connectivity.ui.screens.updates.AppUpdateScreen
import com.zeronetwork.connectivity.ui.theme.ZeroNetworkTheme
import com.zeronetwork.connectivity.ui.viewmodel.CallViewModel
import com.zeronetwork.connectivity.ui.viewmodel.ChatViewModel
import com.zeronetwork.connectivity.ui.viewmodel.MainViewModel
import com.zeronetwork.connectivity.ui.viewmodel.SettingsViewModel

sealed interface AppNavScreen {
    data object Splash : AppNavScreen
    data object Permissions : AppNavScreen
    data object Main : AppNavScreen
    data class ChatDetail(val conversationId: String, val title: String, val isGroup: Boolean) : AppNavScreen
    data object Settings : AppNavScreen
    data object AppUpdates : AppNavScreen
}

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val callViewModel: CallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isFirstLaunch = ZeroNetworkApp.instance.preferencesManager.isFirstLaunch()

        setContent {
            val userProfile by settingsViewModel.userProfile.collectAsState()
            val activeCallSession by callViewModel.callSession.collectAsState()

            var currentScreen by remember {
                mutableStateOf<AppNavScreen>(AppNavScreen.Splash)
            }

            ZeroNetworkTheme(fontKey = userProfile.fontStyleKey) {
                // If username is blank after splash, show compulsory setup
                if (currentScreen != AppNavScreen.Splash && userProfile.username.isBlank()) {
                    UsernameSetupDialog(
                        initialUsername = "",
                        onUsernameConfirmed = { confirmedName ->
                            settingsViewModel.updateUsername(confirmedName)
                        }
                    )
                }

                val currentCall = activeCallSession
                if (currentCall != null) {
                    if (currentCall.isVideo) {
                        VideoCallScreen(
                            viewModel = callViewModel,
                            onCallEnded = { callViewModel.endCall() }
                        )
                    } else {
                        VoiceCallScreen(
                            viewModel = callViewModel,
                            onCallEnded = { callViewModel.endCall() }
                        )
                    }
                } else {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            (slideInHorizontally(animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f)) { width -> width } + fadeIn(tween(250)))
                                .togetherWith(slideOutHorizontally(animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f)) { width -> -width / 3 } + fadeOut(tween(200)))
                        },
                        label = "app_navigation"
                    ) { screen ->
                        when (screen) {
                            is AppNavScreen.Splash -> {
                                SplashScreen(
                                    onSplashFinished = {
                                        currentScreen = if (isFirstLaunch) AppNavScreen.Permissions else AppNavScreen.Main
                                    }
                                )
                            }

                            is AppNavScreen.Permissions -> {
                                PermissionRequestScreen(
                                    onPermissionsGranted = {
                                        currentScreen = AppNavScreen.Main
                                    }
                                )
                            }

                            is AppNavScreen.Main -> {
                                MainScreen(
                                    viewModel = mainViewModel,
                                    onOpenSettings = {
                                        currentScreen = AppNavScreen.Settings
                                    },
                                    onNavigateToUpdates = {
                                        currentScreen = AppNavScreen.AppUpdates
                                    },
                                    onConversationClick = { convId, title, isGroup ->
                                        currentScreen = AppNavScreen.ChatDetail(convId, title, isGroup)
                                    },
                                    onStartVoiceCall = { peer ->
                                        callViewModel.startCall(peer.ipAddress, peer.username, false)
                                    },
                                    onStartVideoCall = { peer ->
                                        callViewModel.startCall(peer.ipAddress, peer.username, true)
                                    }
                                )
                            }

                            is AppNavScreen.ChatDetail -> {
                                ChatDetailScreen(
                                    conversationId = screen.conversationId,
                                    peerTitle = screen.title,
                                    isGroup = screen.isGroup,
                                    onBack = {
                                        currentScreen = AppNavScreen.Main
                                    },
                                    onStartVoiceCall = { ip, title ->
                                        callViewModel.startCall(ip, title, false)
                                    },
                                    onStartVideoCall = { ip, title ->
                                        callViewModel.startCall(ip, title, true)
                                    }
                                )
                            }

                            is AppNavScreen.Settings -> {
                                SettingsScreen(
                                    onNavigateBack = {
                                        currentScreen = AppNavScreen.Main
                                    },
                                    viewModel = settingsViewModel
                                )
                            }

                            is AppNavScreen.AppUpdates -> {
                                AppUpdateScreen(
                                    updateManager = ZeroNetworkApp.instance.updateManager,
                                    onNavigateBack = {
                                        currentScreen = AppNavScreen.Main
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
