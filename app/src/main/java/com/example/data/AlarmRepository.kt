package com.example.data

import com.example.alarm.AlarmScheduler
import kotlinx.coroutines.flow.Flow

interface AlarmScheduleCoordinator {
    fun schedule(alarm: AlarmEntity)
    fun cancel(alarmId: Long)
}

class AndroidAlarmScheduleCoordinator(
    private val scheduler: AlarmScheduler
) : AlarmScheduleCoordinator {
    override fun schedule(alarm: AlarmEntity) = scheduler.schedule(alarm)
    override fun cancel(alarmId: Long) = scheduler.cancel(alarmId)
}

class AlarmRepository(
    private val alarmDao: AlarmDao,
    private val scheduler: AlarmScheduleCoordinator,
    private val settingsDataStore: SettingsDataStore? = null
) {
    constructor(
        alarmDao: AlarmDao,
        scheduler: AlarmScheduler,
        settingsDataStore: SettingsDataStore
    ) : this(
        alarmDao = alarmDao,
        scheduler = AndroidAlarmScheduleCoordinator(scheduler),
        settingsDataStore = settingsDataStore
    )

    val alarmsFlow: Flow<List<AlarmEntity>> = alarmDao.observeAlarms()
    val enabledAlarmsFlow: Flow<List<AlarmEntity>> = alarmDao.observeEnabledAlarms()
    val settingsFlow: Flow<AlarmSettings>
        get() = requireNotNull(settingsDataStore) {
            "SettingsDataStore is required to observe settings"
        }.settingsFlow

    fun observeAlarmById(id: Long): Flow<AlarmEntity?> = alarmDao.observeAlarmById(id)

    suspend fun getAlarmById(id: Long): AlarmEntity? = alarmDao.getAlarmById(id)

    suspend fun createAlarm(
        hour: Int,
        minute: Int,
        label: String = "",
        repeatDaysMask: Int = 0,
        isEnabled: Boolean = true,
        vibrate: Boolean = true,
        gradualWake: Boolean = true,
        snoozeMinutes: Int = 10,
        ringtoneUri: String = "",
        ringtoneTitle: String = "Default system alarm"
    ): AlarmEntity {
        return saveAlarm(
            AlarmEntity(
                hour = hour.coerceIn(0, 23),
                minute = minute.coerceIn(0, 59),
                label = label.trim(),
                isEnabled = isEnabled,
                repeatDaysMask = repeatDaysMask,
                ringtoneUri = ringtoneUri,
                ringtoneTitle = ringtoneTitle,
                vibrate = vibrate,
                gradualWake = gradualWake,
                snoozeMinutes = snoozeMinutes
            )
        )
    }

    suspend fun saveAlarm(alarm: AlarmEntity): AlarmEntity {
        val cleaned = alarm.copy(
            hour = alarm.hour.coerceIn(0, 23),
            minute = alarm.minute.coerceIn(0, 59),
            label = alarm.label.trim(),
            snoozeUntilMillis = null
        )
        val savedId = if (cleaned.id == 0L) {
            alarmDao.insertAlarm(cleaned)
        } else {
            alarmDao.updateAlarm(cleaned)
            cleaned.id
        }
        val persisted = cleaned.copy(id = savedId)
        if (persisted.isEnabled) {
            scheduler.schedule(persisted)
        } else {
            scheduler.cancel(persisted.id)
        }
        return persisted
    }

    suspend fun updateTime(alarmId: Long, hour: Int, minute: Int): AlarmEntity? {
        val existing = alarmDao.getAlarmById(alarmId) ?: return null
        return saveAlarm(existing.copy(hour = hour, minute = minute))
    }

    suspend fun updateLabel(alarmId: Long, label: String): AlarmEntity? {
        val existing = alarmDao.getAlarmById(alarmId) ?: return null
        return saveAlarm(existing.copy(label = label))
    }

    suspend fun updateRepeatSchedule(alarmId: Long, repeatDaysMask: Int): AlarmEntity? {
        val existing = alarmDao.getAlarmById(alarmId) ?: return null
        return saveAlarm(existing.copy(repeatDaysMask = repeatDaysMask))
    }

    suspend fun toggleAlarm(alarm: AlarmEntity, enabled: Boolean) {
        val updated = alarm.copy(
            isEnabled = enabled,
            snoozeUntilMillis = null
        )
        alarmDao.updateAlarm(updated)
        if (enabled) {
            scheduler.schedule(updated)
        } else {
            scheduler.cancel(updated.id)
        }
    }

    suspend fun deleteAlarm(alarm: AlarmEntity) {
        deleteAlarmById(alarm.id)
    }

    suspend fun deleteAlarmById(alarmId: Long) {
        scheduler.cancel(alarmId)
        alarmDao.deleteAlarmById(alarmId)
    }

    suspend fun snoozeAlarm(alarmId: Long, minutesOverride: Int? = null): Long? {
        val alarm = alarmDao.getAlarmById(alarmId) ?: return null
        val snoozeMins = minutesOverride ?: alarm.snoozeMinutes.takeIf { it > 0 } ?: return null
        val snoozeUntil = System.currentTimeMillis() + snoozeMins * 60_000L
        val updated = alarm.copy(
            isEnabled = true,
            snoozeUntilMillis = snoozeUntil
        )
        alarmDao.updateAlarm(updated)
        scheduler.schedule(updated)
        return snoozeUntil
    }

    suspend fun dismissAlarm(alarmId: Long) {
        val alarm = alarmDao.getAlarmById(alarmId) ?: return
        val updated = if (alarm.repeatDaysMask == 0) {
            alarm.copy(isEnabled = false, snoozeUntilMillis = null)
        } else {
            alarm.copy(isEnabled = true, snoozeUntilMillis = null)
        }
        alarmDao.updateAlarm(updated)
        if (updated.isEnabled) {
            scheduler.schedule(updated)
        } else {
            scheduler.cancel(updated.id)
        }
    }

    suspend fun rescheduleAllEnabledAlarms() {
        val enabled = alarmDao.getEnabledAlarmsOnce()
        enabled.forEach { alarm ->
            scheduler.schedule(alarm)
        }
    }

    suspend fun updateUse24Hour(enabled: Boolean) {
        settingsDataStore?.setUse24Hour(enabled)
    }

    suspend fun updateDefaultSnooze(minutes: Int) {
        settingsDataStore?.setDefaultSnoozeMinutes(minutes)
    }

    suspend fun updateSilenceAfter(minutes: Int) {
        settingsDataStore?.setSilenceAfterMinutes(minutes)
    }

    suspend fun updateDefaultGradualWake(enabled: Boolean) {
        settingsDataStore?.setDefaultGradualWake(enabled)
    }
}
