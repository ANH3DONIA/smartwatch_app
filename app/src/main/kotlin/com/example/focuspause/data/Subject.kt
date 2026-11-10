/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.serialization.Serializable

/**
 * Una materia del estudiante. Su [name] es la etiqueta que se guarda en cada sesión
 * ([SessionRecord.label]), así las estadísticas y el plan se calculan por materia.
 */
@Serializable
data class Subject(
    val id: String,
    val name: String,
    /** Día del examen como epoch day (LocalDate.toEpochDay); null si no hay examen próximo. */
    val examEpochDay: Long? = null,
    /** Sesiones de enfoque que el estudiante quiere dedicar a preparar el examen. */
    val examTargetSessions: Int = DEFAULT_TARGET_SESSIONS
) {
    val examDate: LocalDate? get() = examEpochDay?.let(LocalDate::ofEpochDay)

    companion object {
        const val DEFAULT_TARGET_SESSIONS = 10
        const val MAX_NAME_LENGTH = 30
    }
}

/** Qué debe estudiar hoy el estudiante para una materia con examen. */
data class StudyPlan(
    val subject: Subject,
    /** Días que faltan para el examen; 0 es hoy. */
    val daysLeft: Int,
    val sessionsDone: Int,
    val sessionsTarget: Int,
    /** Sesiones que tocan hoy para ir al ritmo. */
    val plannedToday: Int,
    val doneToday: Int
) {
    val leftToday: Int get() = (plannedToday - doneToday).coerceAtLeast(0)
    val remainingTotal: Int get() = (sessionsTarget - sessionsDone).coerceAtLeast(0)
    val isReady: Boolean get() = remainingTotal == 0
}

object StudyPlanner {

    /**
     * Reparte las sesiones pendientes en los días que quedan (sin contar el día del examen).
     * Devuelve null si la materia no tiene examen o ya pasó.
     */
    fun planFor(subject: Subject, records: List<SessionRecord>, today: LocalDate, zone: ZoneId): StudyPlan? {
        val examDay = subject.examDate ?: return null
        val daysLeft = ChronoUnit.DAYS.between(today, examDay).toInt()
        if (daysLeft < 0) return null

        val mine = records.filter { it.mode == PomodoroMode.FOCUS && it.completed && it.label == subject.name }
        val doneToday = mine.count { Instant.ofEpochMilli(it.startedAtMillis).atZone(zone).toLocalDate() == today }

        val studyDays = daysLeft.coerceAtLeast(1)
        val remainingBeforeToday = (subject.examTargetSessions - (mine.size - doneToday)).coerceAtLeast(0)
        val planned = (remainingBeforeToday + studyDays - 1) / studyDays

        return StudyPlan(
            subject = subject,
            daysLeft = daysLeft,
            sessionsDone = mine.size,
            sessionsTarget = subject.examTargetSessions,
            plannedToday = planned,
            doneToday = doneToday
        )
    }

    /** Planes de todas las materias con examen vigente, del examen más cercano al más lejano. */
    fun plans(subjects: List<Subject>, records: List<SessionRecord>, today: LocalDate, zone: ZoneId): List<StudyPlan> =
        subjects.mapNotNull { planFor(it, records, today, zone) }.sortedBy { it.daysLeft }
}

/** Configuraciones de estudio listas para usar. */
data class StudyPreset(
    val name: String,
    val description: String,
    val focusMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int,
    val longBreakInterval: Int
) {
    fun applyTo(settings: PomodoroSettings): PomodoroSettings = settings.copy(
        focusMinutes = focusMinutes,
        shortBreakMinutes = shortBreakMinutes,
        longBreakMinutes = longBreakMinutes,
        longBreakInterval = longBreakInterval
    )

    fun matches(settings: PomodoroSettings): Boolean =
        settings.focusMinutes == focusMinutes &&
            settings.shortBreakMinutes == shortBreakMinutes &&
            settings.longBreakMinutes == longBreakMinutes &&
            settings.longBreakInterval == longBreakInterval
}

object StudyPresets {
    val all: List<StudyPreset> = listOf(
        StudyPreset("Clásico", "Tareas del día a día", 25, 5, 15, 4),
        StudyPreset("Examen", "Bloques largos para repasar", 50, 10, 30, 3),
        StudyPreset("Lectura", "Textos y apuntes", 40, 10, 20, 3),
        StudyPreset("Sprint", "Cuando tienes poca energía", 15, 3, 10, 4)
    )
}
