package com.example.data.audio

import android.content.Context
import com.example.R
import com.example.domain.audio.AudioEngineType
import com.example.domain.audio.ReminderAudioEngine
import com.example.domain.audio.ReminderAudioSettings
import com.example.speech.ReminderSpeechManager
import com.example.util.LocaleDirectionHelper
import java.util.Locale

class TtsAudioEngine(private val speechManager: ReminderSpeechManager) : ReminderAudioEngine {
    override val type: AudioEngineType = AudioEngineType.TTS

    override fun play(context: Context, habitName: String, langCode: String, settings: ReminderAudioSettings) {
        val locale = getLocale(langCode)
        val localizedContext = LocaleDirectionHelper.getLocalizedContext(context, langCode)
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
        speechManager.stopAll()
    }

    override fun release() {
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
