package com.example.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.domain.audio.AudioEngineType
import com.example.domain.audio.ReminderAudioEngine
import com.example.domain.audio.ReminderAudioSettings

/**
 * Production-ready implementation of Alarm Sound Engine.
 * Uses USAGE_ALARM to respect device Alarm Volume.
 */
class AlarmSoundEngine : ReminderAudioEngine {
    override val type: AudioEngineType = AudioEngineType.ALARM

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null

    override fun play(context: Context, habitName: String, langCode: String, settings: ReminderAudioSettings) {
        Log.d("AUDIO", "[AUDIO] Alarm Playback Started for habit: $habitName")
        playInternal(context, settings)
    }

    override fun playPreview(context: Context, langCode: String, settings: ReminderAudioSettings) {
        Log.d("AUDIO", "[AUDIO] Alarm Preview Started")
        playInternal(context, settings)
    }

    private fun playInternal(context: Context, settings: ReminderAudioSettings) {
        stop()

        try {
            val uri = if (settings.alarmUri.isNotEmpty()) {
                Uri.parse(settings.alarmUri)
            } else {
                android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                // Note: Volume is controlled by the system Alarm stream because of USAGE_ALARM
                isLooping = settings.alarmDurationSeconds == -1
                prepare()
                start()
            }

            if (settings.alarmDurationSeconds > 0) {
                stopRunnable = Runnable { 
                    Log.d("AUDIO", "[AUDIO] Alarm Playback Finished (Duration Expired)")
                    stop() 
                }
                handler.postDelayed(stopRunnable!!, settings.alarmDurationSeconds * 1000L)
            }
        } catch (e: Exception) {
            Log.e("AUDIO", "[AUDIO] Alarm Playback FAILED", e)
        }
    }

    override fun stop() {
        stopRunnable?.let { handler.removeCallbacks(it) }
        stopRunnable = null
        
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e("AUDIO", "[AUDIO] Error stopping Alarm", e)
        } finally {
            mediaPlayer = null
        }
    }

    override fun release() {
        stop()
    }
}
