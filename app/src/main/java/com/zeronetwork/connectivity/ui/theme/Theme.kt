package com.zeronetwork.connectivity.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = SignalBlue,
    onPrimary = Color.White,
    primaryContainer = SignalBlueDark,
    onPrimaryContainer = Color.White,
    secondary = EmeraldGreen,
    onSecondary = Color.White,
    background = ZeroDarkBackground,
    onBackground = Color.White,
    surface = ZeroDarkSurface,
    onSurface = Color.White,
    surfaceVariant = ZeroDarkSurfaceVariant,
    onSurfaceVariant = SlateLight,
    error = CrimsonRed
)

private val LightColorScheme = lightColorScheme(
    primary = SignalBlue,
    onPrimary = Color.White,
    primaryContainer = SignalBlueLight,
    onPrimaryContainer = Color.White,
    secondary = EmeraldGreen,
    onSecondary = Color.White,
    background = ZeroLightBackground,
    onBackground = Color(0xFF0F172A),
    surface = ZeroLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = ZeroLightSurfaceVariant,
    onSurfaceVariant = SlateGray,
    error = CrimsonRed
)

@Composable
fun ZeroNetworkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontKey: String = "DEFAULT",
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getAppTypography(fontKey),
        shapes = Shapes,
        content = content
    )
}
