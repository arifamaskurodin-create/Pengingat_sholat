package com.example.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.RingingScreen
import com.example.ui.theme.CalmAlarmTheme

class AlarmRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockScreenBehavior()

        val initialAlarmId = intent.getLongExtra(AlarmRingingService.EXTRA_ALARM_ID, -1L)

        setContent {
            CalmAlarmTheme {
                val activeAlarm by AlarmRingingService.activeAlarm.collectAsStateWithLifecycle()
                val alarmId = activeAlarm?.alarmId ?: initialAlarmId

                RingingScreen(
                    hour = activeAlarm?.hour ?: 7,
                    minute = activeAlarm?.minute ?: 0,
                    label = activeAlarm?.label ?: getString(com.example.R.string.alarm_ringing),
                    snoozeMinutes = activeAlarm?.snoozeMinutes ?: 10,
                    is24Hour = activeAlarm?.is24Hour ?: false,
                    isPreview = false,
                    onSnooze = {
                        val snoozeIntent = Intent(this, AlarmRingingService::class.java).apply {
                            action = AlarmRingingService.ACTION_SNOOZE
                            putExtra(AlarmRingingService.EXTRA_ALARM_ID, alarmId)
                        }
                        startService(snoozeIntent)
                        finish()
                    },
                    onDismiss = {
                        val dismissIntent = Intent(this, AlarmRingingService::class.java).apply {
                            action = AlarmRingingService.ACTION_DISMISS
                            putExtra(AlarmRingingService.EXTRA_ALARM_ID, alarmId)
                        }
                        startService(dismissIntent)
                        finish()
                    }
                )
            }
        }
    }

    private fun configureLockScreenBehavior() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}
