package com.zeronetwork.connectivity.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.zeronetwork.connectivity.ui.theme.SignalBlue
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarPulseView(
    modifier: Modifier = Modifier,
    color: Color = SignalBlue,
    activePeersCount: Int = 0
) {
    val transition = rememberInfiniteTransition(label = "radar_pulse")

    val pulse1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )

    val pulse2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )

    val pulse3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse3"
    )

    val rotationAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension / 2

            // Static background rings
            drawCircle(color = color.copy(alpha = 0.08f), radius = maxRadius * 0.33f, center = center)
            drawCircle(color = color.copy(alpha = 0.08f), radius = maxRadius * 0.66f, center = center)
            drawCircle(color = color.copy(alpha = 0.08f), radius = maxRadius, center = center)

            // Ring strokes
            drawCircle(color = color.copy(alpha = 0.2f), radius = maxRadius * 0.33f, center = center, style = Stroke(width = 1.5f))
            drawCircle(color = color.copy(alpha = 0.2f), radius = maxRadius * 0.66f, center = center, style = Stroke(width = 1.5f))
            drawCircle(color = color.copy(alpha = 0.2f), radius = maxRadius, center = center, style = Stroke(width = 1.5f))

            // Expanding pulses
            listOf(pulse1, pulse2, pulse3).forEach { progress ->
                val radius = maxRadius * progress
                val alpha = (1f - progress).coerceIn(0f, 0.6f)
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.5f)
                )
            }

            // Rotating sweep radar line
            val rad = Math.toRadians(rotationAngle.toDouble())
            val endX = center.x + (maxRadius * cos(rad)).toFloat()
            val endY = center.y + (maxRadius * sin(rad)).toFloat()
            drawLine(
                color = color.copy(alpha = 0.4f),
                start = center,
                end = Offset(endX, endY),
                strokeWidth = 2f
            )
        }

        // Center Wi-Fi icon
        Icon(
            imageVector = Icons.Default.Wifi,
            contentDescription = "Radar Central",
            tint = color,
            modifier = Modifier.size(32.dp)
        )
    }
}
