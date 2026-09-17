package com.example.auramusic.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

fun buildDynamicColorScheme(seedColor: Color): ColorScheme {
    val lum = seedColor.luminance()
    val onPrimaryColor = if (lum > 0.6f) Color(0xFF090714) else Color.White

    // Tint dark surfaces smoothly with the seed
    val bg = lerp(Color(0xFF090714), seedColor, 0.08f)
    val surf = lerp(Color(0xFF130E26), seedColor, 0.12f)
    val surfVar = lerp(Color(0xFF1C1636), seedColor, 0.18f)
    val primaryCont = lerp(Color(0xFF1F173D), seedColor, 0.35f)
    val sec = lerp(seedColor, Color(0xFF06B6D4), 0.45f)
    val tert = lerp(seedColor, Color(0xFFEC4899), 0.50f)

    return darkColorScheme(
        primary = seedColor,
        onPrimary = onPrimaryColor,
        primaryContainer = primaryCont,
        onPrimaryContainer = Color.White,
        inversePrimary = lerp(seedColor, Color.White, 0.6f),
        secondary = sec,
        onSecondary = Color(0xFF090714),
        secondaryContainer = lerp(Color(0xFF152A38), sec, 0.35f),
        onSecondaryContainer = Color.White,
        tertiary = tert,
        onTertiary = Color.White,
        tertiaryContainer = lerp(Color(0xFF381528), tert, 0.35f),
        onTertiaryContainer = Color.White,
        background = bg,
        onBackground = AuraTextPrimary,
        surface = surf,
        onSurface = AuraTextPrimary,
        surfaceVariant = surfVar,
        onSurfaceVariant = AuraTextSecondary,
        surfaceTint = seedColor,
        inverseSurface = Color(0xFFF3F4F6),
        inverseOnSurface = Color(0xFF111827),
        outline = seedColor.copy(alpha = 0.35f),
        outlineVariant = seedColor.copy(alpha = 0.18f),
        error = AuraError,
        onError = Color.White
    )
}

@Composable
fun AuraMusicTheme(
    seedColor: Color = AuraPrimary,
    useSystemDynamic: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        useSystemDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicDarkColorScheme(context)
        }
        else -> {
            buildDynamicColorScheme(seedColor)
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
