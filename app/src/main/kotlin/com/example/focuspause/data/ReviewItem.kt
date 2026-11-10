/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.data

import java.time.LocalDate
import kotlinx.serialization.Serializable

/**
 * Algo que el estudiante aprendió y debe repasar a intervalos crecientes
 * (repaso espaciado): un día después, luego 2 días y luego 4 días más tarde.
 */
@Serializable
data class ReviewItem(
    val id: String,
    val note: String,
    /** Materia en la que se estudió (puede no tener). */
    val subject: String? = null,
    val createdEpochDay: Long,
    /** Repasos ya hechos, de 0 a [ReviewScheduler.TOTAL_REVIEWS]. */
    val stage: Int = 0,
    /** Día (epoch day) en que toca el siguiente repaso. */
    val dueEpochDay: Long
) {
    val isDone: Boolean get() = stage >= ReviewScheduler.TOTAL_REVIEWS

    fun isDue(todayEpochDay: Long): Boolean = !isDone && dueEpochDay <= todayEpochDay

    /** Número del repaso que toca ahora (1 a [ReviewScheduler.TOTAL_REVIEWS]). */
    val nextReviewNumber: Int get() = (stage + 1).coerceAtMost(ReviewScheduler.TOTAL_REVIEWS)

    companion object {
        const val MAX_NOTE_LENGTH = 140
    }
}

object ReviewScheduler {

    /** Días de espera tras crear el apunte y tras cada repaso: 1, 2 y 4 (en total, repasos a los días 1, 3 y 7). */
    private val GAPS_DAYS = listOf(1, 2, 4)
    val TOTAL_REVIEWS: Int = GAPS_DAYS.size

    fun create(id: String, note: String, subject: String?, today: LocalDate): ReviewItem {
        val day = today.toEpochDay()
        return ReviewItem(
            id = id,
            note = note.trim().take(ReviewItem.MAX_NOTE_LENGTH),
            subject = subject,
            createdEpochDay = day,
            stage = 0,
            dueEpochDay = day + GAPS_DAYS[0]
        )
    }

    /** Registra un repaso hecho hoy y programa el siguiente (o termina el apunte). */
    fun markReviewed(item: ReviewItem, today: LocalDate): ReviewItem {
        if (item.isDone) return item
        val stage = item.stage + 1
        return if (stage >= TOTAL_REVIEWS) {
            item.copy(stage = stage)
        } else {
            item.copy(stage = stage, dueEpochDay = today.toEpochDay() + GAPS_DAYS[stage])
        }
    }

    fun dueToday(items: List<ReviewItem>, todayEpochDay: Long): List<ReviewItem> =
        items.filter { it.isDue(todayEpochDay) }.sortedBy { it.dueEpochDay }

    fun upcoming(items: List<ReviewItem>, todayEpochDay: Long): List<ReviewItem> =
        items.filter { !it.isDone && !it.isDue(todayEpochDay) }.sortedBy { it.dueEpochDay }
}
