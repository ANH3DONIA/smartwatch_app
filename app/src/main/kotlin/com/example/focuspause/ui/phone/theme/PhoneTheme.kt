/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.ui.phone.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.focuspause.data.PomodoroMode

val FocusOrange = Color(0xFFFF6D00)
val FocusOrangeLight = Color(0xFFFF9E80)
val FocusOrangeContainer = Color(0xFF3E1F00)

val ShortBreakGreen = Color(0xFF00C853)
val ShortBreakGreenLight = Color(0xFF69F0AE)
val ShortBreakGreenContainer = Color(0xFF003912)

val LongBreakBlue = Color(0xFF0091EA)
val LongBreakBlueLight = Color(0xFF40C4FF)
val LongBreakBlueContainer = Color(0xFF003254)

fun modeColor(mode: PomodoroMode): Color {
    return when (mode) {
        PomodoroMode.FOCUS -> FocusOrange
        PomodoroMode.SHORT_BREAK -> ShortBreakGreen
        PomodoroMode.LONG_BREAK -> LongBreakBlue
    }
}

fun modeLightColor(mode: PomodoroMode): Color {
    return when (mode) {
        PomodoroMode.FOCUS -> FocusOrangeLight
        PomodoroMode.SHORT_BREAK -> ShortBreakGreenLight
        PomodoroMode.LONG_BREAK -> LongBreakBlueLight
    }
}

fun modeContainerColor(mode: PomodoroMode): Color {
    return when (mode) {
        PomodoroMode.FOCUS -> FocusOrangeContainer
        PomodoroMode.SHORT_BREAK -> ShortBreakGreenContainer
        PomodoroMode.LONG_BREAK -> LongBreakBlueContainer
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = FocusOrange,
    secondary = FocusOrangeLight,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun PhoneFocusPauseTheme(
    mode: PomodoroMode = PomodoroMode.FOCUS,
    content: @Composable () -> Unit
) {
    val primaryColor = modeColor(mode)
    val colorScheme = DarkColorScheme.copy(
        primary = primaryColor,
        secondary = modeLightColor(mode),
        primaryContainer = modeContainerColor(mode)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
