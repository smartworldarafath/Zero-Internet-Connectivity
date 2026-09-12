package com.zeronetwork.connectivity.ui.screens.findothers

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeronetwork.connectivity.data.model.Peer
import com.zeronetwork.connectivity.ui.components.FullscreenImageViewer
import com.zeronetwork.connectivity.ui.components.QrConnectDialog
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FindOthersScreen(
    onlinePeers: List<Peer>,
    ownIp: String?,
    ownUsername: String,
    onPeerClick: (Peer) -> Unit,
    onVoiceCallClick: (Peer) -> Unit,
    onVideoCallClick: (Peer) -> Unit,
    onRefreshScan: () -> Unit,
    onManualConnect: (String) -> Unit
) {
    var showQrDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var lightboxPeer by remember { mutableStateOf<Peer?>(null) }

    val filteredPeers = remember(onlinePeers, selectedFilter) {
        when (selectedFilter) {
            "STRONG" -> onlinePeers.filter { it.signalStrength >= 70 }
            else -> onlinePeers
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Ultra-Modern Futuristic Radar Sonar Sweep
        Box(
            modifier = Modifier
                .size(190.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            RadarSonarView(primaryColor = MaterialTheme.colorScheme.primary)

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${onlinePeers.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Text(
            text = "${onlinePeers.size} Active Peers on LAN",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Subnet: ${ownIp ?: "127.0.0.1"} • Port 1050",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onRefreshScan,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan LAN")
            }

            OutlinedButton(
                onClick = { showQrDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("QR Connect")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("All (${onlinePeers.size})") }
            )
            FilterChip(
                selected = selectedFilter == "STRONG",
                onClick = { selectedFilter = "STRONG" },
                label = { Text("Strong Signal") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Peer Discovered Cards
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filteredPeers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Searching for nearby devices...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Ensure other devices have the app open on the same Wi-Fi/Hotspot.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(filteredPeers, key = { it.ipAddress }) { peer ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onPeerClick(peer) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar (Click to Zoom)
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable { lightboxPeer = peer }
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = peer.username.take(2).uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = peer.username,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = peer.ipAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Signal Bars Indicator
                                    SignalStrengthBadge(strength = peer.signalStrength)
                                }
                            }

                            // Quick Call & Chat Actions
                            IconButton(onClick = { onVoiceCallClick(peer) }) {
                                Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onVideoCallClick(peer) }) {
                                Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onPeerClick(peer) }) {
                                Icon(Icons.Default.Chat, contentDescription = "Chat", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    // QR Connect Dialog
    if (showQrDialog) {
        QrConnectDialog(
            ownIp = ownIp ?: "127.0.0.1",
            ownName = ownUsername,
            onDismiss = { showQrDialog = false },
            onPeerScanned = { targetIp, targetName ->
                showQrDialog = false
                onManualConnect(targetIp)
            }
        )
    }

    // Fullscreen Avatar Lightbox
    if (lightboxPeer != null) {
        val p = lightboxPeer!!
        FullscreenImageViewer(
            title = p.username,
            imageBase64 = p.avatarBase64,
            avatarColorIndex = p.avatarColorIndex,
            onDismiss = { lightboxPeer = null }
        )
    }
}

@Composable
fun SignalStrengthBadge(strength: Int) {
    val color = when {
        strength >= 75 -> Color(0xFF10B981)
        strength >= 40 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
    Row(verticalAlignment = Alignment.Bottom) {
        Box(modifier = Modifier.size(3.dp, 6.dp).background(color, RoundedCornerShape(1.dp)))
        Spacer(modifier = Modifier.width(2.dp))
        Box(modifier = Modifier.size(3.dp, 9.dp).background(if (strength >= 40) color else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(1.dp)))
        Spacer(modifier = Modifier.width(2.dp))
        Box(modifier = Modifier.size(3.dp, 12.dp).background(if (strength >= 75) color else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(1.dp)))
    }
}

@Composable
fun RadarSonarView(primaryColor: Color) {
    val transition = rememberInfiniteTransition(label = "radar")
    val sweepAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    val pulseScale by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.minDimension / 2

        // Radar Rings
        drawCircle(
            color = primaryColor.copy(alpha = 0.15f),
            radius = maxRadius,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = primaryColor.copy(alpha = 0.2f),
            radius = maxRadius * 0.66f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = primaryColor.copy(alpha = 0.25f),
            radius = maxRadius * 0.33f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Pulsating Wave
        drawCircle(
            color = primaryColor.copy(alpha = (1f - pulseScale) * 0.4f),
            radius = maxRadius * pulseScale,
            center = center
        )

        // Sweeping Beam
        val rad = Math.toRadians(sweepAngle.toDouble())
        val endX = center.x + (maxRadius * cos(rad)).toFloat()
        val endY = center.y + (maxRadius * sin(rad)).toFloat()

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(primaryColor.copy(alpha = 0.8f), primaryColor.copy(alpha = 0.1f)),
                start = center,
                end = Offset(endX, endY)
            ),
            start = center,
            end = Offset(endX, endY),
            strokeWidth = 3.dp.toPx()
        )
    }
}
