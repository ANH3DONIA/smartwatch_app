/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.focuspause.data.FocusRepository
import com.example.focuspause.data.FocusState
import com.example.focuspause.data.PomodoroMode
import com.example.focuspause.data.PomodoroSettings
import com.example.focuspause.data.SessionRecord
import com.example.focuspause.data.ReviewItem
import com.example.focuspause.data.ReviewScheduler
import com.example.focuspause.data.StatsCalculator
import com.example.focuspause.data.StudyPreset
import com.example.focuspause.data.Subject
import com.example.focuspause.data.TimerStatus
import com.example.focuspause.haptics.HapticFeedbackProvider
import com.example.focuspause.notifications.TimerAlarmScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class FocusTimerViewModel(
    private val hapticProvider: HapticFeedbackProvider? = null,
    private val repository: FocusRepository? = null,
    private val alarmScheduler: TimerAlarmScheduler? = null,
    private val clock: () -> Long = System::currentTimeMillis,
    /** Se llama cuando cambian datos locales que conviene sincronizar con el otro dispositivo. */
    private val onDataChanged: () -> Unit = {}
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusState())
    val uiState: StateFlow<FocusState> = _uiState.asStateFlow()

    /** Historial de sesiones, para la pantalla de estadísticas. */
    val history: StateFlow<List<SessionRecord>> =
        (repository?.history ?: flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    /** Materias del estudiante, con su examen opcional. */
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _reviews = MutableStateFlow<List<ReviewItem>>(emptyList())
    /** Apuntes pendientes de repasar (repaso espaciado). */
    val reviews: StateFlow<List<ReviewItem>> = _reviews.asStateFlow()

    private var timerJob: Job? = null
    private var sessionStartedAtMillis: Long? = null
    private var settingsTouched = false

    private val haptics: HapticFeedbackProvider?
        get() = if (_uiState.value.settings.vibrationEnabled) hapticProvider else null

    init {
        reloadFromRepository(initial = true)
    }

    /**
     * Vuelve a leer ajustes, materias, repasos y el conteo de hoy desde el almacenamiento.
     * Se llama al arrancar y cuando llegan datos nuevos del otro dispositivo (sincronización).
     */
    fun reloadFromRepository(initial: Boolean = false) {
        val repo = repository ?: return
        viewModelScope.launch {
            val saved = repo.settings.first()
            val subjects = repo.subjects.first()
            val reviews = repo.reviews.first()
            // Al arrancar no se pisa lo que el usuario ya hizo antes de que cargara el disco.
            if (!initial || _subjects.value.isEmpty()) _subjects.value = subjects
            if (!initial || _reviews.value.isEmpty()) _reviews.value = reviews

            val zone = ZoneId.systemDefault()
            val today = StatsCalculator.dailyStats(repo.history.first(), LocalDate.now(zone), zone)
            _uiState.update {
                it.copy(totalCompletedToday = maxOf(it.totalCompletedToday, today.completedFocusSessions))
            }
            if (!initial || !settingsTouched) applySettings(saved)
        }
    }

    /** Alterna entre RUNNING y PAUSED con vibración de clic. */
    fun togglePlayPause() {
        haptics?.vibrateClick()
        if (_uiState.value.isRunning) pauseTimer() else startTimer()
    }

    fun startTimer() {
        if (_uiState.value.isRunning) return

        val now = clock()
        val endsAt = now + _uiState.value.timeRemainingSeconds * 1000L
        if (sessionStartedAtMillis == null) sessionStartedAtMillis = now
        _uiState.update { it.copy(status = TimerStatus.RUNNING, endsAtMillis = endsAt) }
        alarmScheduler?.schedule(endsAt, _uiState.value.mode)

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(TICK_MS)
                // El tiempo restante se calcula contra el reloj, no contando ticks:
                // así no se desvía si la app estuvo en segundo plano.
                val remaining = secondsUntil(endsAt)
                if (remaining > 0) {
                    if (remaining != _uiState.value.timeRemainingSeconds) {
                        _uiState.update { it.copy(timeRemainingSeconds = remaining) }
                    }
                } else {
                    _uiState.update { it.copy(timeRemainingSeconds = 0) }
                    handleSessionFinished()
                    break
                }
            }
        }
    }

    fun pauseTimer() {
        val endsAt = _uiState.value.endsAtMillis
        alarmScheduler?.cancel()
        timerJob?.cancel()
        timerJob = null
        _uiState.update {
            if (it.status == TimerStatus.RUNNING) {
                it.copy(
                    status = TimerStatus.PAUSED,
                    timeRemainingSeconds = endsAt?.let(::secondsUntil) ?: it.timeRemainingSeconds,
                    endsAtMillis = null
                )
            } else {
                it
            }
        }
    }

    fun resetCurrentSession() {
        haptics?.vibrateAction()
        pauseTimer()
        recordCurrentSession(counted = false)
        val current = _uiState.value
        val totalSecs = current.settings.durationForMode(current.mode)
        _uiState.update {
            it.copy(
                status = TimerStatus.IDLE,
                timeRemainingSeconds = totalSecs,
                totalDurationSeconds = totalSecs,
                endsAtMillis = null
            )
        }
    }

    fun skipToNextMode() {
        haptics?.vibrateAction()
        pauseTimer()
        val state = _uiState.value
        // Saltar una sesión de enfoque que ya se había iniciado siempre cuenta
        // (se guarda el tiempo real). Saltar sin haber iniciado no cuenta.
        val counted = state.mode == PomodoroMode.FOCUS && sessionStartedAtMillis != null
        recordCurrentSession(counted = counted)
        transitionToNextMode(wasCompleted = counted)
    }

    private fun handleSessionFinished() {
        val finishedFocus = _uiState.value.mode == PomodoroMode.FOCUS
        if (finishedFocus) haptics?.vibrateFocusCompleted() else haptics?.vibrateBreakCompleted()
        recordCurrentSession(counted = true, finishedNaturally = true)
        transitionToNextMode(wasCompleted = true)
        if (_uiState.value.settings.autoStartNext) startTimer()
    }

    private fun transitionToNextMode(wasCompleted: Boolean) {
        _uiState.update { state ->
            val settings = state.settings
            val isCurrentFocus = state.mode == PomodoroMode.FOCUS

            val (nextMode, nextCycle) = if (isCurrentFocus) {
                if (state.cycleCount >= settings.longBreakInterval) {
                    Pair(PomodoroMode.LONG_BREAK, 1)
                } else {
                    Pair(PomodoroMode.SHORT_BREAK, state.cycleCount)
                }
            } else {
                val cycle = if (state.mode == PomodoroMode.LONG_BREAK) 1 else state.cycleCount + 1
                Pair(PomodoroMode.FOCUS, cycle)
            }

            val nextDuration = settings.durationForMode(nextMode)
            val newCompletedCount = if (wasCompleted && isCurrentFocus) {
                state.totalCompletedToday + 1
            } else {
                state.totalCompletedToday
            }

            state.copy(
                mode = nextMode,
                status = TimerStatus.PAUSED,
                timeRemainingSeconds = nextDuration,
                totalDurationSeconds = nextDuration,
                cycleCount = nextCycle,
                totalCompletedToday = newCompletedCount,
                endsAtMillis = null,
                askWhatLearned = wasCompleted && isCurrentFocus,
                breakCounter = if (nextMode == PomodoroMode.FOCUS) state.breakCounter else state.breakCounter + 1
            )
        }
    }

    /** Guarda en el historial la fase actual (terminada o abandonada) y reinicia su hora de inicio. */
    private fun recordCurrentSession(counted: Boolean, finishedNaturally: Boolean = false) {
        val state = _uiState.value
        val startedAt = sessionStartedAtMillis
        sessionStartedAtMillis = null
        val repo = repository ?: return
        val elapsed = if (finishedNaturally) {
            state.totalDurationSeconds
        } else {
            state.totalDurationSeconds - state.timeRemainingSeconds
        }
        if (startedAt == null || (!counted && elapsed < MIN_ABANDONED_SECONDS)) return

        val record = SessionRecord(
            mode = state.mode,
            startedAtMillis = startedAt,
            plannedSeconds = state.totalDurationSeconds,
            actualSeconds = elapsed,
            completed = counted,
            label = if (state.mode == PomodoroMode.FOCUS) state.label else null
        )
        viewModelScope.launch { repo.addRecord(record); onDataChanged() }
    }

    // --- Repaso espaciado ---

    /** Guarda lo aprendido para repasarlo después; una nota vacía solo cierra la pregunta. */
    fun submitLearned(note: String?) {
        val clean = note?.trim().orEmpty()
        _uiState.update { it.copy(askWhatLearned = false) }
        if (clean.isEmpty()) return
        val item = ReviewScheduler.create(
            id = UUID.randomUUID().toString(),
            note = clean,
            subject = _uiState.value.label,
            today = today()
        )
        persistReviews(_reviews.value + item)
    }

    fun markReviewed(id: String) {
        val today = today()
        persistReviews(_reviews.value.map { if (it.id == id) ReviewScheduler.markReviewed(it, today) else it })
    }

    fun deleteReview(id: String) = persistReviews(_reviews.value.filter { it.id != id })

    private fun persistReviews(list: List<ReviewItem>) {
        _reviews.value = list
        repository?.let { repo -> viewModelScope.launch { repo.saveReviews(list); onDataChanged() } }
    }

    private fun today(): LocalDate =
        Instant.ofEpochMilli(clock()).atZone(ZoneId.systemDefault()).toLocalDate()

    // --- Materias ---

    /** Crea o actualiza una materia (por id). Los nombres no se repiten ni quedan vacíos. */
    fun saveSubject(subject: Subject) {
        val name = subject.name.trim().take(Subject.MAX_NAME_LENGTH)
        if (name.isEmpty()) return
        val clean = subject.copy(
            name = name,
            examTargetSessions = subject.examTargetSessions.coerceIn(1, 100)
        )
        val others = _subjects.value.filter { it.id != clean.id }
        if (others.any { it.name.equals(clean.name, ignoreCase = true) }) return
        persistSubjects(others + clean)
    }

    fun removeSubject(id: String) {
        val removed = _subjects.value.firstOrNull { it.id == id } ?: return
        persistSubjects(_subjects.value.filter { it.id != id })
        if (_uiState.value.label == removed.name) setLabel(null)
    }

    private fun persistSubjects(list: List<Subject>) {
        _subjects.value = list.sortedBy { it.name.lowercase() }
        repository?.let { repo -> viewModelScope.launch { repo.saveSubjects(_subjects.value); onDataChanged() } }
    }

    /** Aplica un perfil de estudio (duraciones e intervalo). */
    fun applyPreset(preset: StudyPreset) = updateSettings { preset.applyTo(it) }

    /** Define en qué se trabaja; null o vacío quita la etiqueta. */
    fun setLabel(label: String?) {
        val clean = label?.trim()?.take(MAX_LABEL_LENGTH)?.takeIf { it.isNotEmpty() }
        _uiState.update { it.copy(label = clean) }
    }

    // --- Ajustes ---

    fun updateSettings(transform: (PomodoroSettings) -> PomodoroSettings) {
        settingsTouched = true
        val newSettings = transform(_uiState.value.settings).sanitized()
        applySettings(newSettings)
        repository?.let { repo -> viewModelScope.launch { repo.saveSettings(newSettings); onDataChanged() } }
    }

    fun updateFocusDuration(minutes: Int) = updateSettings { it.copy(focusMinutes = minutes) }
    fun updateShortBreakDuration(minutes: Int) = updateSettings { it.copy(shortBreakMinutes = minutes) }
    fun updateLongBreakDuration(minutes: Int) = updateSettings { it.copy(longBreakMinutes = minutes) }
    fun updateLongBreakInterval(sessions: Int) = updateSettings { it.copy(longBreakInterval = sessions) }
    fun updateAutoStartNext(enabled: Boolean) = updateSettings { it.copy(autoStartNext = enabled) }
    fun updateVibration(enabled: Boolean) = updateSettings { it.copy(vibrationEnabled = enabled) }
    fun updateKeepScreenOn(enabled: Boolean) = updateSettings { it.copy(keepScreenOn = enabled) }
    fun updateDailyGoal(sessions: Int) = updateSettings { it.copy(dailyGoal = sessions) }

    /** Aplica los ajustes; si la fase actual no ha avanzado, también actualiza su duración. */
    private fun applySettings(settings: PomodoroSettings) {
        _uiState.update { state ->
            val untouched = !state.isRunning && state.timeRemainingSeconds == state.totalDurationSeconds
            val updated = state.copy(settings = settings)
            if (untouched) {
                val secs = settings.durationForMode(state.mode)
                updated.copy(timeRemainingSeconds = secs, totalDurationSeconds = secs)
            } else {
                updated
            }
        }
    }

    private fun secondsUntil(endsAtMillis: Long): Int =
        (((endsAtMillis - clock()) + 999L) / 1000L).coerceAtLeast(0L).toInt()

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    private companion object {
        const val TICK_MS = 250L
        const val MIN_ABANDONED_SECONDS = 60
        const val MAX_LABEL_LENGTH = 30
    }
}
