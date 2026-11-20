package com.eyecare.presentation.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/** Design tokens of the clickable demo (dark theme, warm orange accent). */
object EC {
    val bg = Color(0xFF15171D)
    val surface = Color(0xFF1D2028)
    val surface2 = Color(0xFF242833)
    val line = Color(0xFF2E3340)
    val text = Color(0xFFECE5DA)
    val muted = Color(0xFF9C978E)
    val primary = Color(0xFFF2A65A)
    val primaryInk = Color(0xFF1B1408)
    val primarySoft = Color(0xFF3A2E22)
    val accent = Color(0xFFE8796B)
    val ok = Color(0xFF9CC78A)
    val warn = Color(0xFFF2A65A)
    val miss = Color(0xFF3A3F4C)
    val nav = Color(0xFF111318)
    val band = Color(0xFF4A3A26)
    val bandInk = Color(0xFFF6C58E)
    val okInk = Color(0xFF15171D)
    val winBorder = Color(0x17FFFFFF)

    // Native-looking toast (always dark, Windows 11 style in the demo)
    val toastBg = Color(0xFF2B2B2B)
    val toastBorder = Color(0xFF3D3D3D)
    val toastText = Color(0xFFC5C5C5)
    val toastButton = Color(0xFF3A3A3A)
    val toastButtonBorder = Color(0xFF474747)
}

@Composable
fun EyeCareTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = EC.primary,
            onPrimary = EC.primaryInk,
            background = EC.bg,
            onBackground = EC.text,
            surface = EC.surface,
            onSurface = EC.text,
            surfaceVariant = EC.surface2,
            onSurfaceVariant = EC.muted,
            outline = EC.line,
            error = EC.accent,
        ),
    ) {
        CompositionLocalProvider(LocalContentColor provides EC.text, content = content)
    }
}
