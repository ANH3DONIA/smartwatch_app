/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.ui.phone.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focuspause.data.FocusState
import com.example.focuspause.data.SessionRecord
import com.example.focuspause.data.StatsCalculator
import com.example.focuspause.ui.phone.theme.FocusOrange
import com.example.focuspause.ui.phone.theme.LongBreakBlue
import com.example.focuspause.ui.phone.theme.ShortBreakGreen
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneStatsScreen(
    state: FocusState,
    history: List<SessionRecord>,
    onNavigateBack: () -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val week = remember(history) { StatsCalculator.lastDays(history, today, zone, 7) }
    val streak = remember(history) { StatsCalculator.streak(history, today, zone) }
    val todayStats = week.last()
    val totalSessions = remember(history) {
        history.count { it.completed && it.mode == com.example.focuspause.data.PomodoroMode.FOCUS }
    }
    val goal = state.settings.dailyGoal
    val labelStats = remember(history) { StatsCalculator.byLabel(history) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Meta de hoy
            StatsCard {
                Text("Meta de hoy", fontWeight = FontWeight.Bold, color = FocusOrange, fontSize = 16.sp)
                Text(
                    text = "${todayStats.completedFocusSessions} de $goal sesiones",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
                )
                LinearProgressIndicator(
                    progress = { (todayStats.completedFocusSessions.toFloat() / goal).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = FocusOrange,
                    trackColor = Color(0xFF333333)
                )
            }

            // Resumen en tres números
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Racha", "$streak d", Modifier.weight(1f))
                MetricCard("Hoy", "${todayStats.focusMinutes} min", Modifier.weight(1f))
                MetricCard("Total", "$totalSessions", Modifier.weight(1f))
            }

            // Por etiqueta (materia o tarea)
            if (labelStats.isNotEmpty()) {
                StatsCard {
                    Text("Por materia o tarea", fontWeight = FontWeight.Bold, color = LongBreakBlue, fontSize = 16.sp)
                    val maxSeconds = labelStats.maxOf { it.focusSeconds }.coerceAtLeast(1)
                    labelStats.take(6).forEach { item ->
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.label, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                                Text(
                                    "${item.sessions} ses · ${item.focusMinutes} min",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            LinearProgressIndicator(
                                progress = { item.focusSeconds.toFloat() / maxSeconds },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .height(6.dp),
                                color = LongBreakBlue,
                                trackColor = Color(0xFF333333)
                            )
                        }
                    }
                }
            }

            // Últimos 7 días
            StatsCard {
                Text("Últimos 7 días", fontWeight = FontWeight.Bold, color = ShortBreakGreen, fontSize = 16.sp)
                val maxSessions = (week.maxOf { it.completedFocusSessions }).coerceAtLeast(1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    week.forEach { day ->
                        val fraction = day.completedFocusSessions.toFloat() / maxSessions
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        ) {
                            Text(
                                text = "${day.completedFocusSessions}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .height((70 * fraction).dp.coerceAtLeast(4.dp))
                                    .fillMaxWidth(0.5f)
                                    .background(
                                        if (day.date == today) FocusOrange else Color(0xFF555555),
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                            Text(
                                text = day.date.dayOfWeek
                                    .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                    .take(3),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
