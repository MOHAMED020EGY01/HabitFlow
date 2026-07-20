package com.example.core.audio

import android.content.Context
import android.util.Log
import com.example.R
import com.example.core.audio.AudioEngineType
import com.example.core.audio.ReminderAudioEngine
import com.example.core.audio.ReminderAudioSettings
import com.example.core.audio.ReminderSpeechManager
import com.example.core.util.LocaleDirectionHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Production-ready implementation of Text To Speech Engine.
 * Bridges to the existing ReminderSpeechManager for consistent logic.
 */
class TextToSpeechEngine(private val speechManager: ReminderSpeechManager) : ReminderAudioEngine {
    override val type: AudioEngineType = AudioEngineType.TTS

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: Flow<Boolean> = _isPlaying.asStateFlow()

    override fun play(context: Context, habitName: String, langCode: String, settings: ReminderAudioSettings) {
        Log.d("TTS", "[TTS] Speaking habit reminder: $habitName")
        val locale = getLocale(langCode)
        val localizedContext = LocaleDirectionHelper.getLocalizedContext(context, langCode)
        
        // Language check is handled by the speechManager/engine internally, 
        // but we log the intent here for debugging.
        
        val template = localizedContext.getString(R.string.reminder_voice_template)
        val textToSpeak = try {
            String.format(locale, template, habitName)
        } catch (e: Exception) {
            habitName
        }

        var requestsPending = settings.ttsRepeats.coerceAtLeast(1)
        _isPlaying.value = true

        repeat(requestsPending) {
            speechManager.speak(
                ReminderSpeechManager.SpeechRequest(
                    text = textToSpeak,
                    locale = locale,
                    voiceVolume = settings.voiceVolume,
                    pitch = settings.pitch,
                    rate = settings.rate,
                    onDone = {
                        requestsPending--
                        if (requestsPending <= 0) {
                            Log.d("TTS", "[TTS] All speech requests finished")
                            _isPlaying.value = false
                        }
                    }
                )
            )
        }
    }

    override fun playPreview(context: Context, langCode: String, settings: ReminderAudioSettings) {
        Log.d("TTS", "[TTS] Speaking preview")
        val locale = getLocale(langCode)
        val localizedContext = LocaleDirectionHelper.getLocalizedContext(context, langCode)
        val textToSpeak = localizedContext.getString(R.string.reminder_voice_preview)

        _isPlaying.value = true
        speechManager.speak(
            ReminderSpeechManager.SpeechRequest(
                text = textToSpeak,
                locale = locale,
                voiceVolume = settings.voiceVolume,
                pitch = settings.pitch,
                rate = settings.rate,
                onDone = {
                    Log.d("TTS", "[TTS] Preview finished")
                    _isPlaying.value = false
                }
            )
        )
    }

    override fun updateVolume(volume: Float) {
        // TTS volume is set per-request in speak(), live updates are not supported by Android TTS API
        // without restarting the speech. We log this as a limitation or ignore.
    }

    override fun stop() {
        Log.d("TTS", "[TTS] Stopping playback")
        _isPlaying.value = false
        speechManager.stopAll()
    }

    override fun release() {
        Log.d("TTS", "[TTS] Releasing resources")
        speechManager.release()
    }

    private fun getLocale(langCode: String): Locale {
        return if (langCode == "system") {
            Locale.getDefault()
        } else {
            Locale.forLanguageTag(langCode)
        }
    }
}
