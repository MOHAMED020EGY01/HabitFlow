package com.example.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.example.domain.audio.AudioEngineType
import com.example.domain.audio.ReminderAudioEngine
import com.example.domain.audio.ReminderAudioSettings

class AlarmAudioEngine : ReminderAudioEngine {
    override val type: AudioEngineType = AudioEngineType.ALARM

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null

    override fun play(context: Context, habitName: String, langCode: String, settings: ReminderAudioSettings) {
        playInternal(context, settings)
    }

    override fun playPreview(context: Context, langCode: String, settings: ReminderAudioSettings) {
        playInternal(context, settings)
    }

    private fun playInternal(context: Context, settings: ReminderAudioSettings) {
        stop()

        try {
            val uri = if (settings.alarmUri.isNotEmpty()) {
                Uri.parse(settings.alarmUri)
            } else {
                android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setVolume(settings.volume, settings.volume)
                isLooping = settings.alarmDurationSeconds == -1
                prepare()
                start()
            }

            if (settings.alarmDurationSeconds > 0) {
                stopRunnable = Runnable { stop() }
                handler.postDelayed(stopRunnable!!, settings.alarmDurationSeconds * 1000L)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun stop() {
        stopRunnable?.let { handler.removeCallbacks(it) }
        stopRunnable = null
        
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
        }
    }

    override fun release() {
        stop()
    }
}
