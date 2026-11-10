package com.example.focuspause.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncMergerTest {

    private fun rec(startedAt: Long, id: Long = startedAt) = SessionRecord(
        id = id, mode = PomodoroMode.FOCUS, startedAtMillis = startedAt,
        plannedSeconds = 1500, actualSeconds = 1500, completed = true
    )

    @Test fun historyIsUnitedWithoutDuplicates() {
        val phone = SyncPayload(history = listOf(rec(1000), rec(2000)))
        val watch = SyncPayload(history = listOf(rec(2000, id = 99), rec(3000)))
        val merged = SyncMerger.merge(phone, watch)
        assertEquals(listOf(1000L, 2000L, 3000L), merged.history.map { it.startedAtMillis })
    }

    @Test fun newestSettingsWin() {
        val old = SyncPayload(settings = PomodoroSettings(focusMinutes = 25), settingsUpdatedAt = 100)
        val new = SyncPayload(settings = PomodoroSettings(focusMinutes = 50), settingsUpdatedAt = 200)
        assertEquals(50, SyncMerger.merge(old, new).settings.focusMinutes)
        assertEquals(50, SyncMerger.merge(new, old).settings.focusMinutes)
    }

    @Test fun reviewWithMoreProgressWins() {
        val base = ReviewItem("r", "Tema", null, createdEpochDay = 0, stage = 0, dueEpochDay = 1)
        val further = base.copy(stage = 1, dueEpochDay = 3)
        val merged = SyncMerger.merge(SyncPayload(reviews = listOf(base)), SyncPayload(reviews = listOf(further)))
        assertEquals(1, merged.reviews.single().stage)
    }

    @Test fun subjectsAreUnitedByNameIgnoringCase() {
        val a = SyncPayload(subjects = listOf(Subject("1", "Cálculo")))
        val b = SyncPayload(subjects = listOf(Subject("2", "cálculo"), Subject("3", "Inglés")))
        assertEquals(2, SyncMerger.merge(a, b).subjects.size)
    }

    @Test fun mergeIsCommutativeAndIdempotent() {
        val a = SyncPayload(
            settings = PomodoroSettings(focusMinutes = 30), settingsUpdatedAt = 5,
            history = listOf(rec(1000), rec(3000)),
            subjects = listOf(Subject("1", "Cálculo")),
            reviews = listOf(ReviewItem("r1", "A", null, 0, 0, 1))
        )
        val b = SyncPayload(
            settings = PomodoroSettings(focusMinutes = 45), settingsUpdatedAt = 9,
            history = listOf(rec(2000)),
            subjects = listOf(Subject("2", "Física")),
            reviews = listOf(ReviewItem("r2", "B", null, 1, 0, 2))
        )
        val ab = SyncMerger.merge(a, b)
        assertEquals(ab, SyncMerger.merge(b, a))
        assertEquals(ab, SyncMerger.merge(ab, ab))
        assertEquals(ab, SyncMerger.merge(ab, a))
    }
}
