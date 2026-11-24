package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId <= 0L) return

        when (intent.action) {
            ACTION_TRIGGER_ALARM -> {
                val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
                    action = AlarmRingingService.ACTION_START_RINGING
                    putExtra(AlarmRingingService.EXTRA_ALARM_ID, alarmId)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            }
            ACTION_SNOOZE_ALARM -> {
                val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
                    action = AlarmRingingService.ACTION_SNOOZE
                    putExtra(AlarmRingingService.EXTRA_ALARM_ID, alarmId)
                }
                context.startService(serviceIntent)
            }
            ACTION_DISMISS_ALARM -> {
                val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
                    action = AlarmRingingService.ACTION_DISMISS
                    putExtra(AlarmRingingService.EXTRA_ALARM_ID, alarmId)
                }
                context.startService(serviceIntent)
            }
        }
    }

    companion object {
        const val ACTION_TRIGGER_ALARM = "com.example.alarm.ACTION_TRIGGER_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.alarm.ACTION_SNOOZE_ALARM"
        const val ACTION_DISMISS_ALARM = "com.example.alarm.ACTION_DISMISS_ALARM"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }
}
