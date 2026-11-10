/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.ui.wear

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.wear.compose.foundation.AmbientMode
import androidx.wear.compose.foundation.LocalAmbientModeManager
import androidx.wear.compose.foundation.rememberAmbientModeManager
import androidx.wear.compose.material3.AppScaffold
import com.example.focuspause.AppScreen
import com.example.focuspause.ui.screens.AmbientTimerScreen
import com.example.focuspause.ui.screens.SettingsScreen
import com.example.focuspause.ui.screens.TimerMainScreen
import com.example.focuspause.ui.theme.FocusPauseTheme
import com.example.focuspause.viewmodel.FocusTimerViewModel

@Composable
fun WearFocusPauseApp(
    viewModel: FocusTimerViewModel
) {
    val ambientModeManager = rememberAmbientModeManager()

    CompositionLocalProvider(LocalAmbientModeManager provides ambientModeManager) {
        val currentAmbientMode = LocalAmbientModeManager.current?.currentAmbientMode
        val isAmbient = currentAmbientMode is AmbientMode.Ambient

        val uiState by viewModel.uiState.collectAsState()
        var currentScreen by remember { mutableStateOf(AppScreen.TIMER) }

        AppScaffold {
            FocusPauseTheme(
                mode = uiState.mode,
                isAmbient = isAmbient
            ) {
                if (isAmbient) {
                    AmbientTimerScreen(state = uiState)
                } else {
                    when (currentScreen) {
                        // Las estadísticas solo existen en el celular por ahora: aquí cae al temporizador.
                        AppScreen.TIMER, AppScreen.STATS, AppScreen.REVIEWS -> TimerMainScreen(
                            state = uiState,
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onReset = viewModel::resetCurrentSession,
                            onSkip = viewModel::skipToNextMode,
                            onOpenSettings = { currentScreen = AppScreen.SETTINGS }
                        )
                        AppScreen.SETTINGS -> SettingsScreen(
                            state = uiState,
                            onUpdateSettings = viewModel::updateSettings,
                            onNavigateBack = { currentScreen = AppScreen.TIMER }
                        )
                    }
                }
            }
        }
    }
}
