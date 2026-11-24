package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AlarmDatabase
import com.example.data.AlarmRepository
import com.example.data.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AlarmDatabase.getInstance(context)
                    val repository = AlarmRepository(
                        alarmDao = db.alarmDao(),
                        scheduler = AlarmScheduler(context),
                        settingsDataStore = SettingsDataStore(context)
                    )
                    repository.rescheduleAllEnabledAlarms()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
