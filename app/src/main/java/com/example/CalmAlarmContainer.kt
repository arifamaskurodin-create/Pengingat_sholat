package com.example

import android.content.Context
import com.example.alarm.AlarmScheduler
import com.example.alarm.AlarmToneManager
import com.example.data.AlarmDatabase
import com.example.data.AlarmRepository
import com.example.data.SettingsDataStore

class CalmAlarmContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database by lazy { AlarmDatabase.getInstance(appContext) }
    val settingsDataStore by lazy { SettingsDataStore(appContext) }
    val alarmScheduler by lazy { AlarmScheduler(appContext) }
    val alarmToneManager by lazy { AlarmToneManager(appContext) }
    val alarmRepository by lazy {
        AlarmRepository(
            alarmDao = database.alarmDao(),
            scheduler = alarmScheduler,
            settingsDataStore = settingsDataStore
        )
    }
}
