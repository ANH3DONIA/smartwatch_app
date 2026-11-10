/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.viewmodel

import com.example.focuspause.data.FocusRepository
import com.example.focuspause.data.PomodoroMode
import com.example.focuspause.data.PomodoroSettings
import com.example.focuspause.data.ReviewItem
import com.example.focuspause.data.SessionRecord
import com.example.focuspause.data.SyncPayload
import com.example.focuspause.data.StudyPresets
import com.example.focuspause.data.Subject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import com.example.focuspause.data.TimerStatus
import com.example.focuspause.haptics.HapticFeedbackProvider
import com.example.focuspause.notifications.TimerAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeHapticProvider : HapticFeedbackProvider {
    var clickCount = 0
    var focusCompletedCount = 0
    var breakCompletedCount = 0
    var actionCount = 0

    override fun vibrateClick() { clickCount++ }
    override fun vibrateFocusCompleted() { focusCompletedCount++ }
    override fun vibrateBreakCompleted() { breakCompletedCount++ }
    override fun vibrateAction() { actionCount++ }
}

class FakeAlarmScheduler : TimerAlarmScheduler {
    var scheduledAt: Long? = null
    var cancelCount = 0
    override fun schedule(endsAtMillis: Long, mode: PomodoroMode) { scheduledAt = endsAtMillis }
    override fun cancel() { scheduledAt = null; cancelCount++ }
}

class FakeRepository : FocusRepository {
    val settingsFlow = MutableStateFlow(PomodoroSettings())
    val records = mutableListOf<SessionRecord>()
    val subjectsFlow = MutableStateFlow<List<Subject>>(emptyList())
    val reviewsFlow = MutableStateFlow<List<ReviewItem>>(emptyList())
    override val reviews: Flow<List<ReviewItem>> = reviewsFlow
    override suspend fun saveReviews(reviews: List<ReviewItem>) { reviewsFlow.value = reviews }
    override val subjects: Flow<List<Subject>> = subjectsFlow
    override suspend fun saveSubjects(subjects: List<Subject>) { subjectsFlow.value = subjects }
    override val settings: Flow<PomodoroSettings> = settingsFlow
    override val history: Flow<List<SessionRecord>> get() = flowOf(records.toList())
    override suspend fun saveSettings(settings: PomodoroSettings) { settingsFlow.value = settings }
    override suspend fun snapshot() =
        SyncPayload(settingsFlow.value, 0L, records.toList(), subjectsFlow.value, reviewsFlow.value)
    override suspend fun applySnapshot(payload: SyncPayload) {
        settingsFlow.value = payload.settings
        records.clear(); records.addAll(payload.history)
        subjectsFlow.value = payload.subjects
        reviewsFlow.value = payload.reviews
    }
    override suspend fun addRecord(record: SessionRecord) { records += record }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FocusTimerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeHaptics: FakeHapticProvider
    private lateinit var viewModel: FocusTimerViewModel
    private lateinit var fakeRepo: FakeRepository
    private val fakeAlarm = FakeAlarmScheduler()

    // Reloj virtual: avanza con advanceTimeBy, igual que delay().
    private fun newViewModel() = FocusTimerViewModel(
        hapticProvider = fakeHaptics,
        repository = fakeRepo,
        alarmScheduler = fakeAlarm,
        clock = { testDispatcher.scheduler.currentTime }
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeHaptics = FakeHapticProvider()
        fakeRepo = FakeRepository()
        viewModel = newViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        val state = viewModel.uiState.value
        assertEquals(PomodoroMode.FOCUS, state.mode)
        assertEquals(TimerStatus.IDLE, state.status)
        assertEquals(25 * 60, state.timeRemainingSeconds)
        assertEquals(25 * 60, state.totalDurationSeconds)
        assertEquals(1, state.cycleCount)
        assertEquals(0, state.totalCompletedToday)
        assertFalse(state.isRunning)
        assertEquals("25:00", state.formattedTime)
    }

    @Test
    fun testTogglePlayPauseStartsAndPauses() = runTest {
        // First click -> start
        viewModel.togglePlayPause()
        assertEquals(TimerStatus.RUNNING, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.isRunning)
        assertEquals(1, fakeHaptics.clickCount)

        // Advance 3 seconds
        advanceTimeBy(3000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(25 * 60 - 3, viewModel.uiState.value.timeRemainingSeconds)

        // Second click -> pause
        viewModel.togglePlayPause()
        assertEquals(TimerStatus.PAUSED, viewModel.uiState.value.status)
        assertFalse(viewModel.uiState.value.isRunning)
        assertEquals(2, fakeHaptics.clickCount)
    }

    @Test
    fun testResetCurrentSession() = runTest {
        viewModel.startTimer()
        advanceTimeBy(10000L)
        testDispatcher.scheduler.runCurrent()

        viewModel.resetCurrentSession()
        val state = viewModel.uiState.value
        assertEquals(TimerStatus.IDLE, state.status)
        assertEquals(25 * 60, state.timeRemainingSeconds)
        assertEquals(1, fakeHaptics.actionCount)
    }

    @Test
    fun testSkipToNextModeFromFocusToShortBreak() {
        viewModel.skipToNextMode()
        val state = viewModel.uiState.value
        assertEquals(PomodoroMode.SHORT_BREAK, state.mode)
        assertEquals(TimerStatus.PAUSED, state.status)
        assertEquals(5 * 60, state.timeRemainingSeconds)
        assertEquals(1, state.cycleCount)
        assertEquals(1, fakeHaptics.actionCount)
    }

    @Test
    fun testSkipFromShortBreakBackToFocusIncrementsCycle() {
        // Skip Focus -> Short Break
        viewModel.skipToNextMode()
        assertEquals(PomodoroMode.SHORT_BREAK, viewModel.uiState.value.mode)

        // Skip Short Break -> Next Focus
        viewModel.skipToNextMode()
        val state = viewModel.uiState.value
        assertEquals(PomodoroMode.FOCUS, state.mode)
        assertEquals(2, state.cycleCount)
    }

    @Test
    fun testUpdateDurations() {
        viewModel.updateFocusDuration(30)
        assertEquals(30, viewModel.uiState.value.settings.focusMinutes)
        assertEquals(30 * 60, viewModel.uiState.value.timeRemainingSeconds)

        viewModel.updateShortBreakDuration(10)
        assertEquals(10, viewModel.uiState.value.settings.shortBreakMinutes)
    }

    @Test
    fun testFocusCompletesAndIsRecorded() = runTest {
        viewModel.updateFocusDuration(1)
        viewModel.startTimer()
        advanceTimeBy(61_000L)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(PomodoroMode.SHORT_BREAK, state.mode)
        assertEquals(1, state.totalCompletedToday)
        assertEquals(1, fakeHaptics.focusCompletedCount)
        assertEquals(1, fakeRepo.records.size)
        assertTrue(fakeRepo.records[0].completed)
        assertEquals(60, fakeRepo.records[0].actualSeconds)
    }

    @Test
    fun testAutoStartNextBreak() = runTest {
        viewModel.updateSettings { it.copy(focusMinutes = 1, autoStartNext = true) }
        viewModel.startTimer()
        advanceTimeBy(61_000L)
        testDispatcher.scheduler.runCurrent()

        assertEquals(PomodoroMode.SHORT_BREAK, viewModel.uiState.value.mode)
        assertTrue(viewModel.uiState.value.isRunning)
        viewModel.pauseTimer()
    }

    @Test
    fun testPauseKeepsRemainingTime() = runTest {
        viewModel.startTimer()
        advanceTimeBy(5_000L)
        testDispatcher.scheduler.runCurrent()
        viewModel.pauseTimer()
        advanceTimeBy(30_000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals(25 * 60 - 5, viewModel.uiState.value.timeRemainingSeconds)
    }

    @Test
    fun testSettingsArePersistedAndSanitized() = runTest {
        viewModel.updateFocusDuration(500)
        testDispatcher.scheduler.runCurrent()
        assertEquals(90, viewModel.uiState.value.settings.focusMinutes)
        assertEquals(90, fakeRepo.settingsFlow.value.focusMinutes)
    }

    @Test
    fun testVibrationDisabledSilencesHaptics() {
        viewModel.updateVibration(false)
        viewModel.togglePlayPause()
        assertEquals(0, fakeHaptics.clickCount)
        viewModel.pauseTimer()
    }

    @Test
    fun testAlarmScheduledOnStartAndCancelledOnPause() = runTest {
        viewModel.startTimer()
        assertEquals(25 * 60 * 1000L, fakeAlarm.scheduledAt)
        viewModel.pauseTimer()
        assertEquals(null, fakeAlarm.scheduledAt)
    }

    @Test
    fun testBreakActivityChangesBetweenBreaks() {
        assertEquals(null, viewModel.uiState.value.breakActivity)
        viewModel.skipToNextMode()                       // enfoque -> pausa corta
        val first = viewModel.uiState.value.breakActivity
        viewModel.skipToNextMode()                       // pausa -> enfoque
        assertEquals(null, viewModel.uiState.value.breakActivity)
        viewModel.skipToNextMode()                       // enfoque -> pausa corta
        val second = viewModel.uiState.value.breakActivity
        assertTrue(first != null && second != null && first != second)
    }

    @Test
    fun testLabelIsTrimmedAndSavedWithSession() = runTest {
        viewModel.setLabel("  Cálculo  ")
        assertEquals("Cálculo", viewModel.uiState.value.label)
        viewModel.updateFocusDuration(1)
        viewModel.startTimer()
        advanceTimeBy(61_000L)
        testDispatcher.scheduler.runCurrent()
        assertEquals("Cálculo", fakeRepo.records.single().label)
        viewModel.setLabel("   ")
        assertEquals(null, viewModel.uiState.value.label)
    }

    @Test
    fun testSubjectsAreSavedWithoutDuplicates() = runTest {
        viewModel.saveSubject(Subject(id = "1", name = "  Cálculo  "))
        viewModel.saveSubject(Subject(id = "2", name = "cálculo"))   // duplicado (ignora mayúsculas)
        viewModel.saveSubject(Subject(id = "3", name = "   "))       // vacío
        testDispatcher.scheduler.runCurrent()
        assertEquals(listOf("Cálculo"), viewModel.subjects.value.map { it.name })
        assertEquals(1, fakeRepo.subjectsFlow.value.size)

        viewModel.setLabel("Cálculo")
        viewModel.removeSubject("1")
        assertTrue(viewModel.subjects.value.isEmpty())
        assertEquals(null, viewModel.uiState.value.label)
    }

    @Test
    fun testApplyPresetChangesSettings() {
        val exam = StudyPresets.all.first { it.name == "Examen" }
        viewModel.applyPreset(exam)
        val s = viewModel.uiState.value.settings
        assertEquals(50, s.focusMinutes)
        assertEquals(3, s.longBreakInterval)
        assertTrue(exam.matches(s))
    }

    @Test
    fun testSkippingAfterHalfCountsAsSession() = runTest {
        viewModel.updateFocusDuration(1)
        viewModel.startTimer()
        advanceTimeBy(40_000L)                       // 40 s de 60 s
        testDispatcher.scheduler.runCurrent()
        viewModel.skipToNextMode()
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, viewModel.uiState.value.totalCompletedToday)
        val record = fakeRepo.records.single()
        assertTrue(record.completed)
        assertEquals(40, record.actualSeconds)
    }

    @Test
    fun testResettingNeverCountsAsSession() = runTest {
        viewModel.updateFocusDuration(2)
        viewModel.startTimer()
        advanceTimeBy(70_000L)                       // 70 s de 120 s (58%): cuenta
        testDispatcher.scheduler.runCurrent()
        viewModel.pauseTimer()
        viewModel.resetCurrentSession()              // reiniciar nunca cuenta
        testDispatcher.scheduler.runCurrent()

        assertEquals(0, viewModel.uiState.value.totalCompletedToday)
        assertEquals(false, fakeRepo.records.single().completed)
    }

    @Test
    fun testCompletedFocusAsksWhatWasLearnedAndSavesReview() = runTest {
        viewModel.setLabel("Cálculo")
        viewModel.updateFocusDuration(1)
        viewModel.startTimer()
        advanceTimeBy(61_000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.askWhatLearned)

        viewModel.submitLearned("  Regla de la cadena  ")
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.uiState.value.askWhatLearned)
        val review = viewModel.reviews.value.single()
        assertEquals("Regla de la cadena", review.note)
        assertEquals("Cálculo", review.subject)
        assertEquals(1L, review.dueEpochDay - review.createdEpochDay)
        assertEquals(1, fakeRepo.reviewsFlow.value.size)
    }

    @Test
    fun testEmptyLearnedNoteOnlyClosesThePrompt() {
        viewModel.submitLearned("   ")
        assertTrue(viewModel.reviews.value.isEmpty())
        assertFalse(viewModel.uiState.value.askWhatLearned)
    }

    @Test
    fun testMarkReviewedAdvancesAndDeleteRemoves() {
        viewModel.submitLearned("Leyes de Newton")
        val id = viewModel.reviews.value.single().id
        viewModel.markReviewed(id)
        assertEquals(1, viewModel.reviews.value.single().stage)
        viewModel.deleteReview(id)
        assertTrue(viewModel.reviews.value.isEmpty())
    }

    @Test
    fun testSkippingRightAfterStartingStillCounts() = runTest {
        viewModel.startTimer()
        advanceTimeBy(5_000L)
        testDispatcher.scheduler.runCurrent()
        viewModel.skipToNextMode()
        testDispatcher.scheduler.runCurrent()

        assertEquals(1, viewModel.uiState.value.totalCompletedToday)
        assertEquals(PomodoroMode.SHORT_BREAK, viewModel.uiState.value.mode)
        assertTrue(viewModel.uiState.value.askWhatLearned)
        val record = fakeRepo.records.single()
        assertTrue(record.completed)
        assertEquals(5, record.actualSeconds)
    }

    @Test
    fun testSkippingWithoutStartingDoesNotCount() {
        viewModel.skipToNextMode()
        assertEquals(0, viewModel.uiState.value.totalCompletedToday)
        assertFalse(viewModel.uiState.value.askWhatLearned)
    }

    @Test
    fun testReloadPullsDataSyncedFromTheOtherDevice() = runTest {
        testDispatcher.scheduler.runCurrent()
        fakeRepo.reviewsFlow.value = listOf(ReviewItem("r1", "Tema remoto", null, createdEpochDay = 1, dueEpochDay = 2))
        fakeRepo.subjectsFlow.value = listOf(Subject("s1", "Física"))
        fakeRepo.settingsFlow.value = PomodoroSettings(focusMinutes = 45)

        viewModel.reloadFromRepository()
        testDispatcher.scheduler.runCurrent()

        assertEquals(listOf("Tema remoto"), viewModel.reviews.value.map { it.note })
        assertEquals(listOf("Física"), viewModel.subjects.value.map { it.name })
        assertEquals(45, viewModel.uiState.value.settings.focusMinutes)
    }
}
