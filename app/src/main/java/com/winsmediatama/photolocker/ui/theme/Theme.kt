package com.winsmediatama.photolocker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VaultColors = darkColorScheme(
    primary = Color(0xFFB7A5FF),
    secondary = Color(0xFF73E0D3),
    background = Color(0xFF090B12),
    surface = Color(0xFF141722),
    surfaceVariant = Color(0xFF202432),
    onPrimary = Color(0xFF20134D),
    onSecondary = Color(0xFF003731),
    onBackground = Color(0xFFF2F1FA),
    onSurface = Color(0xFFF2F1FA)
)

@Composable
fun PhotoLockerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = VaultColors, content = content)
}
