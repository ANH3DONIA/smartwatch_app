/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.ui.phone

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.focuspause.AppScreen
import com.example.focuspause.data.ReviewScheduler
import com.example.focuspause.data.StatsCalculator
import com.example.focuspause.ui.phone.components.LearnedDialog
import com.example.focuspause.ui.phone.screens.PhoneReviewScreen
import com.example.focuspause.ui.phone.screens.PhoneSettingsScreen
import com.example.focuspause.ui.phone.screens.PhoneStatsScreen
import com.example.focuspause.ui.phone.screens.PhoneTimerMainScreen
import com.example.focuspause.ui.phone.theme.PhoneFocusPauseTheme
import com.example.focuspause.viewmodel.FocusTimerViewModel
import java.time.LocalDate

@Composable
fun PhoneFocusPauseApp(
    viewModel: FocusTimerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.history.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    var currentScreen by remember { mutableStateOf(AppScreen.TIMER) }

    // El botón atrás vuelve al temporizador desde las pantallas secundarias.
    BackHandler(enabled = currentScreen != AppScreen.TIMER) {
        currentScreen = AppScreen.TIMER
    }

    PhoneFocusPauseTheme(mode = uiState.mode) {
        when (currentScreen) {
            AppScreen.TIMER -> PhoneTimerMainScreen(
                state = uiState,
                onTogglePlayPause = viewModel::togglePlayPause,
                onReset = viewModel::resetCurrentSession,
                onSkip = viewModel::skipToNextMode,
                onOpenSettings = { currentScreen = AppScreen.SETTINGS },
                onOpenStats = { currentScreen = AppScreen.STATS },
                recentLabels = remember(history) { StatsCalculator.recentLabels(history) },
                onSelectLabel = viewModel::setLabel,
                dueReviews = remember(reviews) {
                    ReviewScheduler.dueToday(reviews, LocalDate.now().toEpochDay()).size
                },
                onOpenReviews = { currentScreen = AppScreen.REVIEWS }
            )
            AppScreen.SETTINGS -> PhoneSettingsScreen(
                state = uiState,
                onUpdateSettings = viewModel::updateSettings,
                onNavigateBack = { currentScreen = AppScreen.TIMER }
            )
            AppScreen.STATS -> PhoneStatsScreen(
                state = uiState,
                history = history,
                onNavigateBack = { currentScreen = AppScreen.TIMER }
            )
            AppScreen.REVIEWS -> PhoneReviewScreen(
                reviews = reviews,
                onMarkReviewed = viewModel::markReviewed,
                onDelete = viewModel::deleteReview,
                onNavigateBack = { currentScreen = AppScreen.TIMER }
            )
        }

        // Al terminar una sesión de enfoque: ¿qué aprendiste? (alimenta el repaso espaciado)
        if (uiState.askWhatLearned) {
            LearnedDialog(
                subject = uiState.label,
                onSave = viewModel::submitLearned,
                onSkip = { viewModel.submitLearned(null) }
            )
        }
    }
}
