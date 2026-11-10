/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.ui.phone.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focuspause.data.FocusState
import com.example.focuspause.data.PomodoroMode
import com.example.focuspause.data.TimerStatus
import com.example.focuspause.ui.phone.components.CycleRing
import com.example.focuspause.ui.phone.components.LabelDialog
import com.example.focuspause.ui.phone.theme.modeColor
import com.example.focuspause.ui.phone.theme.modeContainerColor
import com.example.focuspause.ui.phone.theme.modeLightColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneTimerMainScreen(
    state: FocusState,
    onTogglePlayPause: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
    recentLabels: List<String>,
    onSelectLabel: (String?) -> Unit,
    dueReviews: Int,
    onOpenReviews: () -> Unit
) {
    var showLabelDialog by remember { mutableStateOf(false) }
    val currentModeColor = modeColor(state.mode)
    val currentModeContainer = modeContainerColor(state.mode)
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress,
        animationSpec = tween(durationMillis = 500),
        label = "progressAnimation"
    )

    if (showLabelDialog) {
        LabelDialog(
            currentLabel = state.label,
            recentLabels = recentLabels,
            onSelect = { onSelectLabel(it); showLabelDialog = false },
            onDismiss = { showLabelDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "FocusPause",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Ciclo ${state.cycleCount} de ${state.settings.longBreakInterval}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenReviews) {
                        BadgedBox(badge = { if (dueReviews > 0) Badge { Text("$dueReviews") } }) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "Repasos pendientes"
                            )
                        }
                    }
                    IconButton(onClick = onOpenStats) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Estadísticas"
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuración"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Mode Chip Badge
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = currentModeContainer,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = state.mode.title,
                    color = currentModeColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Big Circular Countdown Ring
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CycleRing(
                    state = state,
                    progress = animatedProgress,
                    modeColor = currentModeColor,
                    doneColor = modeColor(PomodoroMode.FOCUS),
                    modifier = Modifier.fillMaxSize()
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.formattedTime,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (state.status) {
                            TimerStatus.RUNNING -> "En marcha"
                            TimerStatus.PAUSED -> "Pausado"
                            TimerStatus.IDLE -> "Listo"
                            TimerStatus.COMPLETED -> "¡Completado!"
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Pomodoro Cycles Indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.mode == PomodoroMode.FOCUS) {
                    AssistChip(
                        onClick = { showLabelDialog = true },
                        label = { Text(state.label ?: "¿En qué trabajas?") },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                state.breakActivity?.let { activity ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = currentModeContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Pausa activa: ${activity.title}",
                                color = currentModeColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = activity.instruction,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "Hoy: ${state.totalCompletedToday} de ${state.settings.dailyGoal} sesiones",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.mode == PomodoroMode.FOCUS) {
                    val left = state.settings.longBreakInterval - state.cycleCount + 1
                    Text(
                        text = if (left <= 1) "Esta sesión da paso a la pausa larga"
                        else "Pausa larga en $left sesiones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Control Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button
                FilledTonalIconButton(
                    onClick = onReset,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reiniciar",
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play / Pause Main Button
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(80.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = currentModeColor,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isRunning) "Pausar" else "Iniciar",
                        modifier = Modifier.size(42.dp)
                    )
                }

                // Skip Button
                FilledTonalIconButton(
                    onClick = onSkip,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Saltar",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
