/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.data

import java.time.LocalDate

/** Una fase terminada (o abandonada) del pomodoro. Se guardará en el historial. */
@kotlinx.serialization.Serializable
data class SessionRecord(
    val id: Long = 0,
    val mode: PomodoroMode,
    val startedAtMillis: Long,
    val plannedSeconds: Int,
    val actualSeconds: Int,
    val completed: Boolean,
    val label: String? = null
)

/** Resumen de un día, calculado a partir del historial. */
data class DailyStats(
    val date: LocalDate,
    val completedFocusSessions: Int,
    val focusSeconds: Int
) {
    val focusMinutes: Int get() = focusSeconds / 60
}

/** Tiempo de enfoque acumulado en una etiqueta (materia o tarea). */
data class LabelStats(val label: String, val sessions: Int, val focusSeconds: Int) {
    val focusMinutes: Int get() = focusSeconds / 60
}

object StatsCalculator {

    /** Sesiones de enfoque completadas agrupadas por etiqueta, de más a menos tiempo. */
    fun byLabel(records: List<SessionRecord>): List<LabelStats> =
        records
            .filter { it.mode == PomodoroMode.FOCUS && it.completed && !it.label.isNullOrBlank() }
            .groupBy { it.label!! }
            .map { (label, list) -> LabelStats(label, list.size, list.sumOf { it.actualSeconds }) }
            .sortedByDescending { it.focusSeconds }

    /** Etiquetas usadas, de la más reciente a la más antigua, sin repetir. */
    fun recentLabels(records: List<SessionRecord>, limit: Int = 8): List<String> =
        records.asReversed().mapNotNull { it.label?.takeIf(String::isNotBlank) }.distinct().take(limit)

    fun dailyStats(records: List<SessionRecord>, date: LocalDate, zone: java.time.ZoneId): DailyStats {
        val focus = records.filter {
            it.mode == PomodoroMode.FOCUS &&
                java.time.Instant.ofEpochMilli(it.startedAtMillis).atZone(zone).toLocalDate() == date
        }
        return DailyStats(
            date = date,
            completedFocusSessions = focus.count { it.completed },
            focusSeconds = focus.sumOf { it.actualSeconds }
        )
    }

    /** Estadísticas de los últimos [days] días, del más antiguo al de [today]. */
    fun lastDays(
        records: List<SessionRecord>,
        today: LocalDate,
        zone: java.time.ZoneId,
        days: Int
    ): List<DailyStats> =
        (days - 1 downTo 0).map { dailyStats(records, today.minusDays(it.toLong()), zone) }

    /** Días consecutivos (hasta [today]) con al menos una sesión de enfoque completada. */
    fun streak(records: List<SessionRecord>, today: LocalDate, zone: java.time.ZoneId): Int {
        val days = records
            .filter { it.mode == PomodoroMode.FOCUS && it.completed }
            .map { java.time.Instant.ofEpochMilli(it.startedAtMillis).atZone(zone).toLocalDate() }
            .toSet()
        var day = if (today in days) today else today.minusDays(1)
        var count = 0
        while (day in days) { count++; day = day.minusDays(1) }
        return count
    }
}
