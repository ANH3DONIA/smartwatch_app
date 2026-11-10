/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Almacenamiento de ajustes e historial. Se abstrae para poder probar el ViewModel sin Android. */
interface FocusRepository {
    val settings: Flow<PomodoroSettings>
    val history: Flow<List<SessionRecord>>
    val subjects: Flow<List<Subject>>
    val reviews: Flow<List<ReviewItem>>
    suspend fun saveReviews(reviews: List<ReviewItem>)
    suspend fun saveSubjects(subjects: List<Subject>)
    suspend fun saveSettings(settings: PomodoroSettings)
    /** Copia de todos los datos, para sincronizar con el otro dispositivo. */
    suspend fun snapshot(): SyncPayload
    suspend fun applySnapshot(payload: SyncPayload)
    suspend fun addRecord(record: SessionRecord)
}

private val Context.focusDataStore: DataStore<Preferences> by preferencesDataStore(name = "focuspause")

class DataStoreFocusRepository(context: Context) : FocusRepository {

    private val store = context.applicationContext.focusDataStore
    private val json = Json { ignoreUnknownKeys = true }

    override val settings: Flow<PomodoroSettings> = store.data.map { p ->
        val d = PomodoroSettings()
        PomodoroSettings(
            focusMinutes = p[FOCUS] ?: d.focusMinutes,
            shortBreakMinutes = p[SHORT] ?: d.shortBreakMinutes,
            longBreakMinutes = p[LONG] ?: d.longBreakMinutes,
            longBreakInterval = p[INTERVAL] ?: d.longBreakInterval,
            autoStartNext = p[AUTO_START] ?: d.autoStartNext,
            vibrationEnabled = p[VIBRATION] ?: d.vibrationEnabled,
            keepScreenOn = p[SCREEN_ON] ?: d.keepScreenOn,
            dailyGoal = p[GOAL] ?: d.dailyGoal
        ).sanitized()
    }

    override val history: Flow<List<SessionRecord>> = store.data.map { p -> decodeHistory(p[HISTORY]) }

    override val subjects: Flow<List<Subject>> = store.data.map { p ->
        p[SUBJECTS]?.let { runCatching { json.decodeFromString<List<Subject>>(it) }.getOrNull() }
            ?: emptyList()
    }

    override val reviews: Flow<List<ReviewItem>> = store.data.map { p ->
        p[REVIEWS]?.let { runCatching { json.decodeFromString<List<ReviewItem>>(it) }.getOrNull() }
            ?: emptyList()
    }

    override suspend fun saveReviews(reviews: List<ReviewItem>) {
        store.edit { p -> p[REVIEWS] = json.encodeToString(reviews) }
    }

    override suspend fun saveSubjects(subjects: List<Subject>) {
        store.edit { p -> p[SUBJECTS] = json.encodeToString(subjects) }
    }

    override suspend fun saveSettings(settings: PomodoroSettings) {
        store.edit { p ->
            writeSettings(p, settings)
            p[SETTINGS_UPDATED] = System.currentTimeMillis()
        }
    }

    override suspend fun snapshot(): SyncPayload {
        val prefs = store.data.first()
        return SyncPayload(
            settings = settings.first(),
            settingsUpdatedAt = prefs[SETTINGS_UPDATED] ?: 0L,
            history = decodeHistory(prefs[HISTORY]),
            subjects = prefs[SUBJECTS]
                ?.let { runCatching { json.decodeFromString<List<Subject>>(it) }.getOrNull() }
                ?: emptyList(),
            reviews = prefs[REVIEWS]
                ?.let { runCatching { json.decodeFromString<List<ReviewItem>>(it) }.getOrNull() }
                ?: emptyList()
        )
    }

    override suspend fun applySnapshot(payload: SyncPayload) {
        store.edit { p ->
            writeSettings(p, payload.settings.sanitized())
            p[SETTINGS_UPDATED] = payload.settingsUpdatedAt
            p[HISTORY] = json.encodeToString(payload.history)
            p[SUBJECTS] = json.encodeToString(payload.subjects)
            p[REVIEWS] = json.encodeToString(payload.reviews)
        }
    }

    private fun writeSettings(p: MutablePreferences, settings: PomodoroSettings) {
        p[FOCUS] = settings.focusMinutes
        p[SHORT] = settings.shortBreakMinutes
        p[LONG] = settings.longBreakMinutes
        p[INTERVAL] = settings.longBreakInterval
        p[AUTO_START] = settings.autoStartNext
        p[VIBRATION] = settings.vibrationEnabled
        p[SCREEN_ON] = settings.keepScreenOn
        p[GOAL] = settings.dailyGoal
    }

    override suspend fun addRecord(record: SessionRecord) {
        store.edit { p ->
            val next = (decodeHistory(p[HISTORY]) + record.copy(id = System.currentTimeMillis()))
                .takeLast(MAX_RECORDS)
            p[HISTORY] = json.encodeToString(next)
        }
    }

    private fun decodeHistory(raw: String?): List<SessionRecord> =
        raw?.let { runCatching { json.decodeFromString<List<SessionRecord>>(it) }.getOrNull() }
            ?: emptyList()

    private companion object {
        const val MAX_RECORDS = 500
        val FOCUS = intPreferencesKey("focus_minutes")
        val SHORT = intPreferencesKey("short_break_minutes")
        val LONG = intPreferencesKey("long_break_minutes")
        val INTERVAL = intPreferencesKey("long_break_interval")
        val AUTO_START = booleanPreferencesKey("auto_start_next")
        val VIBRATION = booleanPreferencesKey("vibration_enabled")
        val SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val GOAL = intPreferencesKey("daily_goal")
        val HISTORY = stringPreferencesKey("history_json")
        val SUBJECTS = stringPreferencesKey("subjects_json")
        val REVIEWS = stringPreferencesKey("reviews_json")
        val SETTINGS_UPDATED = longPreferencesKey("settings_updated_at")
    }
}
