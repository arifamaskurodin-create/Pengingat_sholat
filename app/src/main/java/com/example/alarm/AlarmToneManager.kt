package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AlarmToneOption(
    val title: String,
    val uriString: String
)

class AlarmToneManager(private val context: Context) {

    private var previewPlayer: MediaPlayer? = null

    suspend fun loadAlarmTones(): List<AlarmToneOption> = withContext(Dispatchers.IO) {
        val options = mutableListOf(
            AlarmToneOption(title = "Default system alarm", uriString = "")
        )
        runCatching {
            val manager = RingtoneManager(context).apply {
                setType(RingtoneManager.TYPE_ALARM)
            }
            val cursor = manager.cursor
            while (cursor != null && cursor.moveToNext()) {
                val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX).orEmpty()
                val uri = manager.getRingtoneUri(cursor.position)?.toString().orEmpty()
                if (title.isNotBlank() && uri.isNotBlank()) {
                    options.add(AlarmToneOption(title = title, uriString = uri))
                }
            }
        }
        options.distinctBy { it.uriString }
    }

    fun resolveToneUri(uriString: String): Uri? {
        if (uriString.isNotBlank()) {
            return Uri.parse(uriString)
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    }

    fun playPreview(uriString: String) {
        stopPreview()
        val uri = resolveToneUri(uriString) ?: return
        runCatching {
            previewPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                isLooping = true
                prepare()
                start()
            }
        }
    }

    fun stopPreview() {
        runCatching {
            previewPlayer?.apply {
                if (isPlaying) stop()
                reset()
                release()
            }
        }
        previewPlayer = null
    }
}
