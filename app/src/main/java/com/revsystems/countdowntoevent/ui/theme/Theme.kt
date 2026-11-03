package com.revsystems.countdowntoevent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LightColors =
    lightColorScheme(
        primary = Teal500,
        onPrimary = Color.White,
        primaryContainer = Teal100,
        onPrimaryContainer = Ink,
        secondary = Teal700,
        onSecondary = Color.White,
        background = Color.White,
        onBackground = Ink,
        surface = Color.White,
        onSurface = Ink,
        surfaceVariant = CardPlaceholder,
        onSurfaceVariant = InkSoft,
        surfaceContainer = Mist,
        outline = InkSoft,
        error = Error,
    )

val DarkColors =
    darkColorScheme(
        primary = Teal300,
        onPrimary = Ink,
        primaryContainer = TealNightContainer,
        onPrimaryContainer = NightText,
        secondary = Teal300,
        onSecondary = Ink,
        background = NightBackground,
        onBackground = NightText,
        surface = NightBackground,
        onSurface = NightText,
        surfaceVariant = NightSurfaceHigh,
        onSurfaceVariant = NightTextSoft,
        surfaceContainer = NightSurface,
        outline = NightTextSoft,
        error = ErrorNight,
    )

@Composable
fun CountdownTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
