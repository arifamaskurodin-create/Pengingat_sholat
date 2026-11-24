package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.AlarmDatabase
import com.example.data.AlarmEntity
import com.example.data.AlarmRepository
import com.example.data.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActiveRingingAlarm(
    val alarmId: Long,
    val hour: Int,
    val minute: Int,
    val label: String,
    val snoozeMinutes: Int,
    val is24Hour: Boolean
)

class AlarmRingingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaPlayer: MediaPlayer? = null
    private var fadeJob: Job? = null
    private var autoSilenceJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentAlarmId: Long = -1L

    private lateinit var repository: AlarmRepository
    private lateinit var toneManager: AlarmToneManager
    private lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate() {
        super.onCreate()
        val db = AlarmDatabase.getInstance(this)
        settingsDataStore = SettingsDataStore(this)
        repository = AlarmRepository(
            alarmDao = db.alarmDao(),
            scheduler = AlarmScheduler(this),
            settingsDataStore = settingsDataStore
        )
        toneManager = AlarmToneManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(EXTRA_ALARM_ID, -1L) ?: -1L
        when (intent?.action) {
            ACTION_START_RINGING -> {
                if (alarmId > 0L) {
                    currentAlarmId = alarmId
                    startForegroundRinging(alarmId)
                } else {
                    stopSelf()
                }
            }
            ACTION_SNOOZE -> {
                val targetId = if (alarmId > 0L) alarmId else currentAlarmId
                serviceScope.launch {
                    if (targetId > 0L) {
                        repository.snoozeAlarm(targetId)
                    }
                    stopAlertAndShutdown()
                }
            }
            ACTION_DISMISS -> {
                val targetId = if (alarmId > 0L) alarmId else currentAlarmId
                serviceScope.launch {
                    if (targetId > 0L) {
                        repository.dismissAlarm(targetId)
                    }
                    stopAlertAndShutdown()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundRinging(alarmId: Long) {
        val placeholderNotification = buildNotification(
            alarmId = alarmId,
            title = getString(R.string.alarm_ringing),
            subtitle = "",
            snoozeMinutes = 10
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                placeholderNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, placeholderNotification)
        }

        acquireWakeLock()

        serviceScope.launch {
            val alarm = repository.getAlarmById(alarmId)
            val settings = settingsDataStore.getSettingsOnce()
            if (alarm == null) {
                stopAlertAndShutdown()
                return@launch
            }

            val (timeStr, period) = AlarmTimeCalculator.formatTimeParts(
                alarm.hour,
                alarm.minute,
                settings.is24HourFormat
            )
            val formattedTime = if (period != null) "$timeStr $period" else timeStr
            val displayLabel = alarm.label.ifBlank { getString(R.string.alarm_ringing) }

            _activeAlarm.value = ActiveRingingAlarm(
                alarmId = alarm.id,
                hour = alarm.hour,
                minute = alarm.minute,
                label = displayLabel,
                snoozeMinutes = alarm.snoozeMinutes,
                is24Hour = settings.is24HourFormat
            )

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(
                NOTIFICATION_ID,
                buildNotification(
                    alarmId = alarm.id,
                    title = formattedTime,
                    subtitle = displayLabel,
                    snoozeMinutes = alarm.snoozeMinutes
                )
            )

            startAlarmAudio(alarm)
            if (alarm.vibrate) {
                startVibration()
            }
            launchRingingActivity(alarm.id)
            scheduleAutoSilence(alarm.id, settings.silenceAfterMinutes)
        }
    }

    private fun startAlarmAudio(alarm: AlarmEntity) {
        stopAudio()
        val uri = toneManager.resolveToneUri(alarm.ringtoneUri) ?: return
        runCatching {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmRingingService, uri)
                isLooping = true
                prepare()
                if (alarm.gradualWake) {
                    setVolume(0.08f, 0.08f)
                } else {
                    setVolume(1.0f, 1.0f)
                }
                start()
            }
            if (alarm.gradualWake) {
                fadeJob = serviceScope.launch {
                    val steps = 24
                    for (i in 1..steps) {
                        delay(500L)
                        val vol = (0.08f + (0.92f * i / steps)).coerceIn(0f, 1f)
                        runCatching { mediaPlayer?.setVolume(vol, vol) }
                    }
                }
            }
        }
    }

    private fun startVibration() {
        val pattern = longArrayOf(0L, 600L, 500L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }
    }

    private fun stopVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.cancel()
        } else {
            @Suppress("DEPRECATION")
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.cancel()
        }
    }

    private fun stopAudio() {
        fadeJob?.cancel()
        fadeJob = null
        runCatching {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
        }
        mediaPlayer = null
    }

    private fun scheduleAutoSilence(alarmId: Long, silenceAfterMinutes: Int) {
        autoSilenceJob?.cancel()
        if (silenceAfterMinutes <= 0) return
        autoSilenceJob = serviceScope.launch {
            delay(silenceAfterMinutes * 60_000L)
            repository.dismissAlarm(alarmId)
            stopAlertAndShutdown()
        }
    }

    private fun launchRingingActivity(alarmId: Long) {
        val activityIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        runCatching { startActivity(activityIntent) }
    }

    private fun buildNotification(
        alarmId: Long,
        title: String,
        subtitle: String,
        snoozeMinutes: Int
    ): Notification {
        val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            (alarmId.toInt() * 31) + 1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(title)
            .setContentText(subtitle.ifBlank { getString(R.string.alarm_ringing) })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(0, getString(R.string.dismiss), dismissPendingIntent)

        if (snoozeMinutes > 0) {
            val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_SNOOZE_ALARM
                putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                this,
                (alarmId.toInt() * 31) + 2,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "${getString(R.string.snooze)} ${snoozeMinutes}m", snoozePendingIntent)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.alarm_channel_desc)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "CalmAlarm::RingingWakeLock"
        ).apply {
            acquire(15 * 60_000L)
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        }
        wakeLock = null
    }

    private fun stopAlertAndShutdown() {
        autoSilenceJob?.cancel()
        autoSilenceJob = null
        stopAudio()
        stopVibration()
        releaseWakeLock()
        _activeAlarm.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopAudio()
        stopVibration()
        releaseWakeLock()
        _activeAlarm.value = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_RINGING = "com.example.alarm.START_RINGING"
        const val ACTION_SNOOZE = "com.example.alarm.SNOOZE"
        const val ACTION_DISMISS = "com.example.alarm.DISMISS"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        private const val CHANNEL_ID = "calm_alarm_alerts"
        private const val NOTIFICATION_ID = 4102

        private val _activeAlarm = MutableStateFlow<ActiveRingingAlarm?>(null)
        val activeAlarm: StateFlow<ActiveRingingAlarm?> = _activeAlarm.asStateFlow()
    }
}
