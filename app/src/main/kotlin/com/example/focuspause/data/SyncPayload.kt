/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.data

import kotlinx.serialization.Serializable

/** Todo lo que se comparte entre el celular y el reloj. */
@Serializable
data class SyncPayload(
    val settings: PomodoroSettings = PomodoroSettings(),
    /** Cuándo se cambiaron los ajustes por última vez (gana el más reciente). */
    val settingsUpdatedAt: Long = 0L,
    val history: List<SessionRecord> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val reviews: List<ReviewItem> = emptyList()
)

/**
 * Mezcla los datos de dos dispositivos. Es conmutativa y estable (mezclar dos veces da lo mismo),
 * así que ambos dispositivos terminan con los mismos datos sin importar el orden de llegada.
 *
 * Limitación: borrar una materia o un apunte en un dispositivo no se propaga todavía;
 * el otro dispositivo lo conserva y puede volver a aparecer.
 */
object SyncMerger {
    const val MAX_HISTORY = 500

    fun merge(a: SyncPayload, b: SyncPayload): SyncPayload {
        val bWins = b.settingsUpdatedAt > a.settingsUpdatedAt ||
            (b.settingsUpdatedAt == a.settingsUpdatedAt && b.settings.toString() > a.settings.toString())
        val newerSettings = if (bWins) b else a

        val history = (a.history + b.history)
            .groupBy { it.startedAtMillis to it.mode }
            .map { (_, same) -> same.minByOrNull { it.id }!! }
            .sortedWith(compareBy({ it.startedAtMillis }, { it.mode }))
            .takeLast(MAX_HISTORY)

        val subjects = (a.subjects + b.subjects)
            .groupBy { it.name.lowercase() }
            .map { (_, same) -> same.maxByOrNull { it.id }!! }
            .sortedBy { it.name.lowercase() }

        // Un apunte repasado más veces (o con fecha más lejana) va por delante del otro.
        val reviews = (a.reviews + b.reviews)
            .groupBy { it.id }
            .map { (_, same) -> same.maxWithOrNull(compareBy({ it.stage }, { it.dueEpochDay }))!! }
            .sortedWith(compareBy({ it.createdEpochDay }, { it.id }))

        return SyncPayload(
            settings = newerSettings.settings,
            settingsUpdatedAt = newerSettings.settingsUpdatedAt,
            history = history,
            subjects = subjects,
            reviews = reviews
        )
    }
}
