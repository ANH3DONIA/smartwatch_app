package com.example.focuspause.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ModelsTest {
    private val zone = ZoneOffset.UTC
    private fun rec(day: LocalDate, completed: Boolean = true, secs: Int = 1500) = SessionRecord(
        mode = PomodoroMode.FOCUS,
        startedAtMillis = day.atStartOfDay(zone).toInstant().toEpochMilli(),
        plannedSeconds = 1500, actualSeconds = secs, completed = completed
    )

    @Test fun sanitizedClampsValues() {
        val s = PomodoroSettings(focusMinutes = 500, shortBreakMinutes = 0, longBreakInterval = 1, dailyGoal = 99).sanitized()
        assertEquals(90, s.focusMinutes); assertEquals(1, s.shortBreakMinutes)
        assertEquals(2, s.longBreakInterval); assertEquals(20, s.dailyGoal)
    }

    @Test fun goalProgress() {
        val st = FocusState(totalCompletedToday = 4, settings = PomodoroSettings(dailyGoal = 8))
        assertEquals(0.5f, st.goalProgress, 0.001f)
    }

    @Test fun dailyStatsCountsOnlyThatDay() {
        val d = LocalDate.of(2026, 11, 9)
        val stats = StatsCalculator.dailyStats(listOf(rec(d), rec(d, completed = false, secs = 300), rec(d.minusDays(1))), d, zone)
        assertEquals(1, stats.completedFocusSessions); assertEquals(1800, stats.focusSeconds)
    }

    @Test fun streakCountsConsecutiveDays() {
        val d = LocalDate.of(2026, 11, 9)
        val r = listOf(rec(d), rec(d.minusDays(1)), rec(d.minusDays(2)), rec(d.minusDays(4)))
        assertEquals(3, StatsCalculator.streak(r, d, zone))
        assertEquals(3, StatsCalculator.streak(r.drop(1), d, zone))
    }

    @Test fun lastDaysIsOrderedOldestFirst() {
        val d = LocalDate.of(2026, 11, 9)
        val week = StatsCalculator.lastDays(listOf(rec(d), rec(d.minusDays(6))), d, zone, 7)
        assertEquals(7, week.size)
        assertEquals(d.minusDays(6), week.first().date)
        assertEquals(1, week.first().completedFocusSessions)
        assertEquals(d, week.last().date)
        assertEquals(1, week.last().completedFocusSessions)
    }

    @Test fun breakActivitiesMatchBreakKindAndRotate() {
        assertEquals(null, BreakActivities.forMode(PomodoroMode.FOCUS, 0))
        val shorts = (0..20).map { BreakActivities.forMode(PomodoroMode.SHORT_BREAK, it)!! }
        assertEquals(true, shorts.none { it.kind == BreakKind.LONG })
        assertEquals(true, shorts.map { it.id }.toSet().size > 1)
        val longs = (0..20).map { BreakActivities.forMode(PomodoroMode.LONG_BREAK, it)!! }
        assertEquals(true, longs.none { it.kind == BreakKind.SHORT })
        // Rota con ciclo completo: índice y índice + tamaño del pool dan lo mismo.
        assertEquals(shorts[0], BreakActivities.forMode(PomodoroMode.SHORT_BREAK, 0))
    }

    @Test fun labelStatsGroupAndRecent() {
        val d = LocalDate.of(2026, 11, 9)
        val r = listOf(
            rec(d).copy(label = "Cálculo"),
            rec(d).copy(label = "Inglés", actualSeconds = 600),
            rec(d).copy(label = "Cálculo"),
            rec(d).copy(label = "Cálculo", completed = false),
            rec(d)
        )
        val stats = StatsCalculator.byLabel(r)
        assertEquals(listOf("Cálculo", "Inglés"), stats.map { it.label })
        assertEquals(2, stats[0].sessions)
        assertEquals(3000, stats[0].focusSeconds)
        assertEquals(listOf("Inglés", "Cálculo"), StatsCalculator.recentLabels(r.dropLast(2)))
    }

    @Test fun studyPlanSplitsRemainingSessionsAcrossDays() {
        val today = LocalDate.of(2026, 11, 9)
        val subject = Subject("1", "Cálculo", examEpochDay = today.plusDays(5).toEpochDay(), examTargetSessions = 10)
        val plan = StudyPlanner.planFor(subject, emptyList(), today, zone)!!
        assertEquals(5, plan.daysLeft)
        assertEquals(2, plan.plannedToday)       // 10 sesiones / 5 días
        assertEquals(2, plan.leftToday)

        // Una sesión hecha hoy: sigue tocando 2 hoy, queda 1.
        val withOne = listOf(rec(today).copy(label = "Cálculo"))
        val plan2 = StudyPlanner.planFor(subject, withOne, today, zone)!!
        assertEquals(2, plan2.plannedToday)
        assertEquals(1, plan2.leftToday)
        assertEquals(9, plan2.remainingTotal)
    }

    @Test fun studyPlanIgnoresOtherSubjectsAndPastExams() {
        val today = LocalDate.of(2026, 11, 9)
        val subject = Subject("1", "Cálculo", examEpochDay = today.plusDays(2).toEpochDay(), examTargetSessions = 4)
        val other = listOf(rec(today).copy(label = "Inglés"))
        assertEquals(0, StudyPlanner.planFor(subject, other, today, zone)!!.sessionsDone)

        val past = subject.copy(examEpochDay = today.minusDays(1).toEpochDay())
        assertEquals(null, StudyPlanner.planFor(past, emptyList(), today, zone))
        assertEquals(null, StudyPlanner.planFor(subject.copy(examEpochDay = null), emptyList(), today, zone))
    }

    @Test fun examDayPlansEverythingForToday() {
        val today = LocalDate.of(2026, 11, 9)
        val subject = Subject("1", "Física", examEpochDay = today.toEpochDay(), examTargetSessions = 6)
        assertEquals(6, StudyPlanner.planFor(subject, emptyList(), today, zone)!!.plannedToday)
    }

    @Test fun presetsApplyAndMatch() {
        val sprint = StudyPresets.all.first { it.name == "Sprint" }
        val applied = sprint.applyTo(PomodoroSettings(dailyGoal = 5))
        assertEquals(15, applied.focusMinutes)
        assertEquals(5, applied.dailyGoal)       // no toca ajustes ajenos al perfil
        assertEquals(true, sprint.matches(applied))
    }

    @Test fun reviewsAreScheduledAtOneThreeAndSevenDays() {
        val day0 = LocalDate.of(2026, 11, 9)
        var item = ReviewScheduler.create("1", "  Mitosis  ", "Biología", day0)
        assertEquals("Mitosis", item.note)
        assertEquals(day0.plusDays(1).toEpochDay(), item.dueEpochDay)
        assertEquals(false, item.isDue(day0.toEpochDay()))
        assertEquals(true, item.isDue(day0.plusDays(1).toEpochDay()))

        item = ReviewScheduler.markReviewed(item, day0.plusDays(1))
        assertEquals(day0.plusDays(3).toEpochDay(), item.dueEpochDay)      // día 3
        item = ReviewScheduler.markReviewed(item, day0.plusDays(3))
        assertEquals(day0.plusDays(7).toEpochDay(), item.dueEpochDay)      // día 7
        assertEquals(false, item.isDone)
        item = ReviewScheduler.markReviewed(item, day0.plusDays(7))
        assertEquals(true, item.isDone)
        assertEquals(false, item.isDue(day0.plusDays(30).toEpochDay()))
    }

    @Test fun lateReviewReschedulesFromTheDayItWasDone() {
        val day0 = LocalDate.of(2026, 11, 9)
        val item = ReviewScheduler.create("1", "Tema", null, day0)
        val late = ReviewScheduler.markReviewed(item, day0.plusDays(5))    // repasó con retraso
        assertEquals(day0.plusDays(7).toEpochDay(), late.dueEpochDay)
    }

    @Test fun dueAndUpcomingAreSplitAndSorted() {
        val today = LocalDate.of(2026, 11, 9)
        val a = ReviewScheduler.create("a", "A", null, today.minusDays(3))   // vencido
        val b = ReviewScheduler.create("b", "B", null, today.minusDays(1))   // hoy
        val c = ReviewScheduler.create("c", "C", null, today)                // mañana
        val t = today.toEpochDay()
        assertEquals(listOf("a", "b"), ReviewScheduler.dueToday(listOf(c, b, a), t).map { it.id })
        assertEquals(listOf("c"), ReviewScheduler.upcoming(listOf(c, b, a), t).map { it.id })
    }
}
