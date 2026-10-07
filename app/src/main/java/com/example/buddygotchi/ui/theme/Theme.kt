package com.example.buddygotchi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NothingColorScheme = darkColorScheme(
    primary = NothingRed,
    onPrimary = Color.White,
    primaryContainer = NothingRedDim,
    onPrimaryContainer = Color.White,
    secondary = NothingTextSecondary,
    onSecondary = NothingWhite,
    tertiary = NothingRed,
    onTertiary = Color.White,
    background = NothingBlack,
    onBackground = NothingWhite,
    surface = NothingCardSurface,
    onSurface = NothingWhite,
    surfaceVariant = NothingSurfaceVariant,
    onSurfaceVariant = NothingWhite,
    outline = NothingBorder
)

@Composable
fun BuddyGotchiTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NothingColorScheme,
        typography = Typography,
        content = content
    )
}