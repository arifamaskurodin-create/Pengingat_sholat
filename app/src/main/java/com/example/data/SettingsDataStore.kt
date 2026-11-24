package com.example.data

import android.content.Context
import android.text.format.DateFormat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "calm_alarm_settings"
)

data class AlarmSettings(
    val is24HourFormat: Boolean = false,
    val defaultSnoozeMinutes: Int = 10,
    val silenceAfterMinutes: Int = 15,
    val defaultGradualWake: Boolean = true
)

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val USE_24_HOUR = booleanPreferencesKey("use_24_hour")
        val DEFAULT_SNOOZE_MIN = intPreferencesKey("default_snooze_min")
        val SILENCE_AFTER_MIN = intPreferencesKey("silence_after_min")
        val DEFAULT_GRADUAL_WAKE = booleanPreferencesKey("default_gradual_wake")
    }

    private val systemDefault24Hour: Boolean
        get() = DateFormat.is24HourFormat(context)

    val settingsFlow: Flow<AlarmSettings> = context.settingsDataStore.data.map { prefs ->
        AlarmSettings(
            is24HourFormat = prefs[Keys.USE_24_HOUR] ?: systemDefault24Hour,
            defaultSnoozeMinutes = prefs[Keys.DEFAULT_SNOOZE_MIN] ?: 10,
            silenceAfterMinutes = prefs[Keys.SILENCE_AFTER_MIN] ?: 15,
            defaultGradualWake = prefs[Keys.DEFAULT_GRADUAL_WAKE] ?: true
        )
    }

    suspend fun getSettingsOnce(): AlarmSettings = settingsFlow.first()

    suspend fun setUse24Hour(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.USE_24_HOUR] = enabled }
    }

    suspend fun setDefaultSnoozeMinutes(minutes: Int) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_SNOOZE_MIN] = minutes }
    }

    suspend fun setSilenceAfterMinutes(minutes: Int) {
        context.settingsDataStore.edit { it[Keys.SILENCE_AFTER_MIN] = minutes }
    }

    suspend fun setDefaultGradualWake(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_GRADUAL_WAKE] = enabled }
    }
}
