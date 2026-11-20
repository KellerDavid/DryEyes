package com.eyecare.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.eyecare.domain.model.AppTheme

private val Light = lightColorScheme(
    primary = Color(0xFF2E7D6B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7EBDB),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF4A635C),
    secondaryContainer = Color(0xFFCCE8DF),
    background = Color(0xFFF6FBF8),
    surface = Color(0xFFF6FBF8),
    surfaceVariant = Color(0xFFDBE5E0),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8CD5BF),
    onPrimary = Color(0xFF00382D),
    primaryContainer = Color(0xFF005142),
    onPrimaryContainer = Color(0xFFA8F2DB),
    secondary = Color(0xFFB1CCC3),
    secondaryContainer = Color(0xFF334B44),
    background = Color(0xFF0F1513),
    surface = Color(0xFF0F1513),
    surfaceVariant = Color(0xFF3F4945),
)

@Composable
fun EyeCareTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val dark = when (theme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
