package com.example.data.audio

import android.content.Context
import android.util.Log
import com.example.R
import com.example.domain.audio.AudioEngineType
import com.example.domain.audio.ReminderAudioEngine
import com.example.domain.audio.ReminderAudioSettings
import com.example.speech.ReminderSpeechManager
import com.example.util.LocaleDirectionHelper
import java.util.Locale

/**
 * Production-ready implementation of Text To Speech Engine.
 * Bridges to the existing ReminderSpeechManager for consistent logic.
 */
class TextToSpeechEngine(private val speechManager: ReminderSpeechManager) : ReminderAudioEngine {
    override val type: AudioEngineType = AudioEngineType.TTS

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

        repeat(settings.ttsRepeats.coerceAtLeast(1)) {
            speechManager.speak(
                ReminderSpeechManager.SpeechRequest(
                    text = textToSpeak,
                    locale = locale,
                    volume = settings.volume,
                    pitch = settings.pitch,
                    rate = settings.rate
                )
            )
        }
    }

    override fun playPreview(context: Context, langCode: String, settings: ReminderAudioSettings) {
        Log.d("TTS", "[TTS] Speaking preview")
        val locale = getLocale(langCode)
        val localizedContext = LocaleDirectionHelper.getLocalizedContext(context, langCode)
        val textToSpeak = localizedContext.getString(R.string.reminder_voice_preview)

        speechManager.speak(
            ReminderSpeechManager.SpeechRequest(
                text = textToSpeak,
                locale = locale,
                volume = settings.volume,
                pitch = settings.pitch,
                rate = settings.rate
            )
        )
    }

    override fun stop() {
        Log.d("TTS", "[TTS] Stopping playback")
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
