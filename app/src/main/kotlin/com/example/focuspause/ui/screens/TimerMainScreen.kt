/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.example.focuspause.data.FocusState
import com.example.focuspause.ui.components.CircularTimerRing
import com.example.focuspause.ui.components.QuickActionButtons

@Composable
fun TimerMainScreen(
    state: FocusState,
    onTogglePlayPause: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenScaffold(
        timeText = { TimeText() },
        modifier = modifier
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Circular Progress Ring hugging outer round bezel
            CircularTimerRing(
                progress = state.progress,
                isAmbient = false,
                activeColor = MaterialTheme.colorScheme.primary
            )

            // Centered Controls & Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                // Mode Title Tag
                Text(
                    text = state.mode.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Big Digital Countdown Display (e.g. 25:00)
                Text(
                    text = state.formattedTime,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Cycle dots/counter
                Text(
                    text = "Ciclo ${state.cycleCount}/${state.settings.longBreakInterval}  •  Hoy: ${state.totalCompletedToday}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                // Pausa activa sugerida (solo el título: la pantalla del reloj es pequeña)
                state.breakActivity?.let { activity ->
                    Text(
                        text = activity.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Action Buttons (Reset, Play/Pause, Skip)
                QuickActionButtons(
                    isRunning = state.isRunning,
                    onTogglePlayPause = onTogglePlayPause,
                    onReset = onReset,
                    onSkip = onSkip
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Settings quick shortcut button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(24.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Ajustes de tiempo",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
