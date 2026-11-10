/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.data

import java.util.Locale

@kotlinx.serialization.Serializable
enum class PomodoroMode(val title: String) {
    FOCUS("ENFOQUE"),
    SHORT_BREAK("PAUSA CORTA"),
    LONG_BREAK("PAUSA LARGA")
}

enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

@kotlinx.serialization.Serializable
data class PomodoroSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val longBreakInterval: Int = 4,
    val autoStartNext: Boolean = false,
    val vibrationEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val dailyGoal: Int = 8
) {
    fun durationForMode(mode: PomodoroMode): Int {
        return when (mode) {
            PomodoroMode.FOCUS -> focusMinutes * 60
            PomodoroMode.SHORT_BREAK -> shortBreakMinutes * 60
            PomodoroMode.LONG_BREAK -> longBreakMinutes * 60
        }
    }

    /** Devuelve una copia con todos los valores dentro de rangos válidos. */
    fun sanitized(): PomodoroSettings = copy(
        focusMinutes = focusMinutes.coerceIn(MIN_MINUTES, MAX_FOCUS_MINUTES),
        shortBreakMinutes = shortBreakMinutes.coerceIn(MIN_MINUTES, MAX_BREAK_MINUTES),
        longBreakMinutes = longBreakMinutes.coerceIn(MIN_MINUTES, MAX_BREAK_MINUTES),
        longBreakInterval = longBreakInterval.coerceIn(2, 8),
        dailyGoal = dailyGoal.coerceIn(1, 20)
    )

    companion object {
        const val MIN_MINUTES = 1
        const val MAX_FOCUS_MINUTES = 90
        const val MAX_BREAK_MINUTES = 60
    }
}

data class FocusState(
    val mode: PomodoroMode = PomodoroMode.FOCUS,
    val status: TimerStatus = TimerStatus.IDLE,
    val timeRemainingSeconds: Int = 25 * 60,
    val totalDurationSeconds: Int = 25 * 60,
    val cycleCount: Int = 1,
    val totalCompletedToday: Int = 0,
    val settings: PomodoroSettings = PomodoroSettings(),
    /** Instante (epoch ms) en que termina la fase; null si no está corriendo. Base para un timer fiable. */
    val endsAtMillis: Long? = null,
    /** Cuántas pausas han empezado; sirve para rotar las pausas activas. */
    val breakCounter: Int = 0,
    /** Materia o tarea en la que se está trabajando (opcional). */
    val label: String? = null,
    /** true justo después de contar una sesión de enfoque: la UI pregunta qué se aprendió. */
    val askWhatLearned: Boolean = false
) {
    /** Pausa activa sugerida para la fase actual (null durante el enfoque). */
    val breakActivity: BreakActivity?
        get() = BreakActivities.forMode(mode, breakCounter)

    val goalProgress: Float
        get() = (totalCompletedToday.toFloat() / settings.dailyGoal).coerceIn(0f, 1f)

    val isRunning: Boolean get() = status == TimerStatus.RUNNING

    val progress: Float
        get() = if (totalDurationSeconds > 0) {
            val elapsed = totalDurationSeconds - timeRemainingSeconds
            (elapsed.toFloat() / totalDurationSeconds.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val formattedTime: String
        get() {
            val minutes = timeRemainingSeconds / 60
            val seconds = timeRemainingSeconds % 60
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }

    val formattedMinutesOnly: String
        get() {
            val minutes = (timeRemainingSeconds + 59) / 60
            return "${minutes}m"
        }
}
