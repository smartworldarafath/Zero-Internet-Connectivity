package com.zeronetwork.connectivity.ui.screens.call

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeronetwork.connectivity.data.model.CallState
import com.zeronetwork.connectivity.ui.theme.AvatarColors
import com.zeronetwork.connectivity.ui.theme.CrimsonRed
import com.zeronetwork.connectivity.ui.theme.EmeraldGreen
import com.zeronetwork.connectivity.ui.theme.SignalBlue
import com.zeronetwork.connectivity.ui.theme.ZeroDarkBackground
import com.zeronetwork.connectivity.ui.viewmodel.CallViewModel

@Composable
fun VoiceCallScreen(
    viewModel: CallViewModel,
    onCallEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session by viewModel.callSession.collectAsState()

    if (session == null) {
        onCallEnded()
        return
    }

    val callState = session!!.state
    val durationSec = session!!.durationSeconds
    val isMuted = session!!.isMuted
    val isSpeakerOn = session!!.isSpeakerOn

    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF0D1B2A)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Peer info & call status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Text(
                    text = session!!.peerName,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "LAN Peer • ${session!!.peerIp}",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.7f))
                )
                Spacer(modifier = Modifier.height(8.dp))

                val statusText = when (callState) {
                    CallState.OUTGOING_CALLING -> "Calling..."
                    CallState.INCOMING_RINGING -> "Incoming Call..."
                    CallState.CONNECTED -> {
                        val mins = durationSec / 60
                        val secs = durationSec % 60
                        String.format("%02d:%02d", mins, secs)
                    }
                    CallState.ENDED -> "Call Ended"
                    CallState.REJECTED -> "Call Declined"
                    CallState.BUSY -> "Busy"
                    else -> ""
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = if (callState == CallState.CONNECTED) EmeraldGreen else SignalBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            // Central Animated Avatar Pulse
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                if (callState == CallState.OUTGOING_CALLING || callState == CallState.INCOMING_RINGING) {
                    Box(
                        modifier = Modifier
                            .size(160.dp * pulseScale)
                            .clip(CircleShape)
                            .background(SignalBlue.copy(alpha = 0.2f))
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = SignalBlue,
                    modifier = Modifier.size(110.dp),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = session!!.peerName.take(1).uppercase().ifEmpty { "U" },
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 44.sp,
                                color = Color.White
                            )
                        )
                    }
                }
            }

            // Bottom Call Controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                if (callState == CallState.INCOMING_RINGING) {
                    // Accept or Decline buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Decline
                        IconButton(
                            onClick = { viewModel.rejectCall() },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(CrimsonRed)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Decline",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // Accept
                        IconButton(
                            onClick = { viewModel.acceptCall() },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(EmeraldGreen)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Accept",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                } else {
                    // Connected / In-call controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mute
                        IconButton(
                            onClick = { viewModel.toggleMute() },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) Color.White else Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mute",
                                tint = if (isMuted) Color.Black else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // End Call
                        IconButton(
                            onClick = { viewModel.endCall() },
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(CrimsonRed)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "End Call",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Speaker
                        IconButton(
                            onClick = { viewModel.toggleSpeaker() },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isSpeakerOn) Color.White else Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                contentDescription = "Speaker",
                                tint = if (isSpeakerOn) Color.Black else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
