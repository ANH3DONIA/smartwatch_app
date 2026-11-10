/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import com.example.focuspause.data.PomodoroMode

fun getModeColorScheme(mode: PomodoroMode, isAmbient: Boolean): ColorScheme {
    if (isAmbient) {
        return ColorScheme(
            primary = AmbientText,
            primaryContainer = AmbientBackground,
            onPrimary = AmbientBackground,
            onPrimaryContainer = AmbientText,
            secondary = AmbientMuted,
            secondaryContainer = AmbientBackground,
            onSecondary = AmbientText,
            background = AmbientBackground,
            onBackground = AmbientText,
            surfaceContainer = AmbientBackground,
            onSurface = AmbientText,
            onSurfaceVariant = AmbientMuted
        )
    }

    return when (mode) {
        PomodoroMode.FOCUS -> ColorScheme(
            primary = FocusPrimary,
            primaryContainer = FocusContainer,
            onPrimary = FocusOnPrimary,
            onPrimaryContainer = FocusPrimary,
            secondary = OnSurfaceSecondary,
            background = Color.Black,
            onBackground = OnSurfaceWhite,
            surfaceContainer = SurfaceDark,
            onSurface = OnSurfaceWhite
        )
        PomodoroMode.SHORT_BREAK -> ColorScheme(
            primary = ShortBreakPrimary,
            primaryContainer = ShortBreakContainer,
            onPrimary = ShortBreakOnPrimary,
            onPrimaryContainer = ShortBreakPrimary,
            secondary = OnSurfaceSecondary,
            background = Color.Black,
            onBackground = OnSurfaceWhite,
            surfaceContainer = SurfaceDark,
            onSurface = OnSurfaceWhite
        )
        PomodoroMode.LONG_BREAK -> ColorScheme(
            primary = LongBreakPrimary,
            primaryContainer = LongBreakContainer,
            onPrimary = LongBreakOnPrimary,
            onPrimaryContainer = LongBreakPrimary,
            secondary = OnSurfaceSecondary,
            background = Color.Black,
            onBackground = OnSurfaceWhite,
            surfaceContainer = SurfaceDark,
            onSurface = OnSurfaceWhite
        )
    }
}

@Composable
fun FocusPauseTheme(
    mode: PomodoroMode = PomodoroMode.FOCUS,
    isAmbient: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = getModeColorScheme(mode, isAmbient),
        typography = AppTypography,
        content = content
    )
}
