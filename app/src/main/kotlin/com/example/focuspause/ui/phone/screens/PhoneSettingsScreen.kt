/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.ui.phone.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focuspause.data.FocusState
import com.example.focuspause.data.PomodoroSettings
import com.example.focuspause.ui.SettingsOptions
import com.example.focuspause.ui.phone.theme.FocusOrange
import com.example.focuspause.ui.phone.theme.LongBreakBlue
import com.example.focuspause.ui.phone.theme.ShortBreakGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneSettingsScreen(
    state: FocusState,
    onUpdateSettings: ((PomodoroSettings) -> PomodoroSettings) -> Unit,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val settings = state.settings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OptionCard(
                title = "Tiempo de Enfoque",
                description = "Duración de cada bloque de concentración.",
                accent = FocusOrange,
                options = SettingsOptions.withCurrent(SettingsOptions.FOCUS_MINUTES, settings.focusMinutes),
                selected = settings.focusMinutes,
                label = { SettingsOptions.minutesLabel(it) },
                onSelect = { v -> onUpdateSettings { it.copy(focusMinutes = v) } }
            )
            OptionCard(
                title = "Pausa Corta",
                description = "Descanso breve entre sesiones de enfoque.",
                accent = ShortBreakGreen,
                options = SettingsOptions.withCurrent(SettingsOptions.SHORT_BREAK_MINUTES, settings.shortBreakMinutes),
                selected = settings.shortBreakMinutes,
                label = { SettingsOptions.minutesLabel(it) },
                onSelect = { v -> onUpdateSettings { it.copy(shortBreakMinutes = v) } }
            )
            OptionCard(
                title = "Pausa Larga",
                description = "Descanso largo al completar un grupo de sesiones.",
                accent = LongBreakBlue,
                options = SettingsOptions.withCurrent(SettingsOptions.LONG_BREAK_MINUTES, settings.longBreakMinutes),
                selected = settings.longBreakMinutes,
                label = { SettingsOptions.minutesLabel(it) },
                onSelect = { v -> onUpdateSettings { it.copy(longBreakMinutes = v) } }
            )
            OptionCard(
                title = "Sesiones hasta la pausa larga",
                description = "Cuántas sesiones de enfoque antes de la pausa larga.",
                accent = LongBreakBlue,
                options = SettingsOptions.withCurrent(SettingsOptions.LONG_BREAK_INTERVAL, settings.longBreakInterval),
                selected = settings.longBreakInterval,
                label = { "$it" },
                onSelect = { v -> onUpdateSettings { it.copy(longBreakInterval = v) } }
            )
            OptionCard(
                title = "Meta diaria",
                description = "Sesiones de enfoque que quieres completar cada día.",
                accent = FocusOrange,
                options = SettingsOptions.withCurrent(SettingsOptions.DAILY_GOAL, settings.dailyGoal),
                selected = settings.dailyGoal,
                label = { "$it" },
                onSelect = { v -> onUpdateSettings { it.copy(dailyGoal = v) } }
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SwitchRow(
                        title = "Iniciar siguiente fase automáticamente",
                        checked = settings.autoStartNext,
                        onChange = { v -> onUpdateSettings { it.copy(autoStartNext = v) } }
                    )
                    SwitchRow(
                        title = "Vibración",
                        checked = settings.vibrationEnabled,
                        onChange = { v -> onUpdateSettings { it.copy(vibrationEnabled = v) } }
                    )
                    SwitchRow(
                        title = "Mantener pantalla encendida",
                        checked = settings.keepScreenOn,
                        onChange = { v -> onUpdateSettings { it.copy(keepScreenOn = v) } }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Método Pomodoro",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = LongBreakBlue
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Trabaja enfocado durante el tiempo seleccionado.\n" +
                            "• Toma una pausa corta después de cada sesión.\n" +
                            "• Tras ${settings.longBreakInterval} sesiones, disfruta de una pausa larga " +
                            "de ${settings.longBreakMinutes} minutos.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionCard(
    title: String,
    description: String,
    accent: Color,
    options: List<Int>,
    selected: Int,
    label: (Int) -> String,
    onSelect: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accent)
            Text(
                text = description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { value ->
                    FilterChip(
                        selected = selected == value,
                        onClick = { onSelect(value) },
                        label = { Text(label(value)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
