/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.example.focuspause.data.FocusState
import com.example.focuspause.data.PomodoroSettings
import com.example.focuspause.ui.SettingsOptions

@Composable
fun SettingsScreen(
    state: FocusState,
    onUpdateSettings: ((PomodoroSettings) -> PomodoroSettings) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    val settings = state.settings
    val focusOptions = SettingsOptions.withCurrent(SettingsOptions.FOCUS_MINUTES, settings.focusMinutes)
    val breakOptions = SettingsOptions.withCurrent(SettingsOptions.SHORT_BREAK_MINUTES, settings.shortBreakMinutes)
    val longBreakOptions = SettingsOptions.withCurrent(SettingsOptions.LONG_BREAK_MINUTES, settings.longBreakMinutes)
    val intervalOptions = SettingsOptions.withCurrent(SettingsOptions.LONG_BREAK_INTERVAL, settings.longBreakInterval)
    val goalOptions = SettingsOptions.withCurrent(SettingsOptions.DAILY_GOAL, settings.dailyGoal)

    ScreenScaffold(
        scrollState = scrollState,
        modifier = modifier
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            item {
                ListHeader(
                    modifier = Modifier
                        .minimumVerticalContentPadding(
                            ListHeaderDefaults.minimumTopListContentPadding,
                            ListHeaderDefaults.minimumBottomListContentPadding
                        )
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text("Ajustes")
                }
            }

            // Focus duration subsection
            item {
                ListSubHeader(
                    modifier = Modifier
                        .minimumVerticalContentPadding(
                            ListHeaderDefaults.minimumTopListContentPadding,
                            ListHeaderDefaults.minimumBottomListContentPadding
                        )
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text("Tiempo de Enfoque")
                }
            }

            items(focusOptions.size) { index ->
                val minutes = focusOptions[index]
                val isSelected = state.settings.focusMinutes == minutes

                Button(
                    onClick = { onUpdateSettings { it.copy(focusMinutes = minutes) } },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    colors = if (isSelected) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    },
                    icon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Seleccionado"
                            )
                        }
                    }
                ) {
                    Text(SettingsOptions.minutesLabel(minutes))
                }
            }

            // Break duration subsection
            item {
                ListSubHeader(
                    modifier = Modifier
                        .minimumVerticalContentPadding(
                            ListHeaderDefaults.minimumTopListContentPadding,
                            ListHeaderDefaults.minimumBottomListContentPadding
                        )
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Text("Pausa Corta")
                }
            }

            items(breakOptions.size) { index ->
                val minutes = breakOptions[index]
                val isSelected = state.settings.shortBreakMinutes == minutes

                Button(
                    onClick = { onUpdateSettings { it.copy(shortBreakMinutes = minutes) } },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    colors = if (isSelected) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    },
                    icon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Seleccionado"
                            )
                        }
                    }
                ) {
                    Text(SettingsOptions.minutesLabel(minutes))
                }
            }

            // Long break + toggles
            item { SectionTitle("Pausa Larga", transformationSpec) }
            items(longBreakOptions.size) { index ->
                val minutes = longBreakOptions[index]
                OptionButton(
                    text = SettingsOptions.minutesLabel(minutes),
                    selected = state.settings.longBreakMinutes == minutes,
                    transformationSpec = transformationSpec,
                    onClick = { onUpdateSettings { it.copy(longBreakMinutes = minutes) } }
                )
            }

            item { SectionTitle("Sesiones hasta la pausa larga", transformationSpec) }
            items(intervalOptions.size) { index ->
                val sessions = intervalOptions[index]
                OptionButton(
                    text = "$sessions sesiones",
                    selected = state.settings.longBreakInterval == sessions,
                    transformationSpec = transformationSpec,
                    onClick = { onUpdateSettings { it.copy(longBreakInterval = sessions) } }
                )
            }

            item { SectionTitle("Meta diaria", transformationSpec) }
            items(goalOptions.size) { index ->
                val sessions = goalOptions[index]
                OptionButton(
                    text = "$sessions sesiones",
                    selected = state.settings.dailyGoal == sessions,
                    transformationSpec = transformationSpec,
                    onClick = { onUpdateSettings { it.copy(dailyGoal = sessions) } }
                )
            }

            item { SectionTitle("Opciones", transformationSpec) }
            item {
                OptionButton(
                    text = "Autoiniciar fases",
                    selected = state.settings.autoStartNext,
                    transformationSpec = transformationSpec,
                    onClick = { onUpdateSettings { it.copy(autoStartNext = !it.autoStartNext) } }
                )
            }
            item {
                OptionButton(
                    text = "Vibración",
                    selected = state.settings.vibrationEnabled,
                    transformationSpec = transformationSpec,
                    onClick = { onUpdateSettings { it.copy(vibrationEnabled = !it.vibrationEnabled) } }
                )
            }
            item {
                OptionButton(
                    text = "Pantalla encendida",
                    selected = state.settings.keepScreenOn,
                    transformationSpec = transformationSpec,
                    onClick = { onUpdateSettings { it.copy(keepScreenOn = !it.keepScreenOn) } }
                )
            }

            // Back button item
            item {
                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    colors = ButtonDefaults.outlinedButtonColors(),
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                ) {
                    Text("Volver")
                }
            }
        }
    }
}

@Composable
private fun TransformingLazyColumnItemScope.SectionTitle(
    text: String,
    transformationSpec: androidx.wear.compose.material3.lazy.TransformationSpec
) {
    ListSubHeader(
        modifier = Modifier
            .minimumVerticalContentPadding(
                ListHeaderDefaults.minimumTopListContentPadding,
                ListHeaderDefaults.minimumBottomListContentPadding
            )
            .transformedHeight(this, transformationSpec),
        transformation = SurfaceTransformation(transformationSpec)
    ) {
        Text(text)
    }
}

/** Botón de opción con check cuando está seleccionado; sirve también como interruptor. */
@Composable
private fun TransformingLazyColumnItemScope.OptionButton(
    text: String,
    selected: Boolean,
    transformationSpec: androidx.wear.compose.material3.lazy.TransformationSpec,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
            .transformedHeight(this, transformationSpec),
        transformation = SurfaceTransformation(transformationSpec),
        colors = if (selected) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
        icon = {
            if (selected) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = "Activado")
            }
        }
    ) {
        Text(text)
    }
}
