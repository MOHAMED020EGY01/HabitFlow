package com.example.speech

import android.content.Context
import com.example.HabitApplication
import com.example.R
import com.example.data.preferences.UserPreferencesManager
import com.example.domain.model.Habit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

/**
 * High-level controller for the Text-to-Speech system.
 */
class ReminderSpeechController(private val context: Context) {

    private val speechManager = ReminderSpeechManager(context)
    private val preferencesManager = UserPreferencesManager(context)

    /**
     * Entry point for habit reminders. Resolves settings and localized text.
     */
    fun speakHabitReminder(habit: Habit) {
        android.util.Log.d("TTS", "[TTS] Reminder Triggered: ${habit.name}")
        val app = context.applicationContext as HabitApplication
        
        runBlocking {
            val isEnabled = preferencesManager.isSpeechEnabledFlow.first()
            if (!isEnabled) {
                android.util.Log.d("TTS", "[TTS] Speech Disabled in settings")
                return@runBlocking
            }

            val volume = preferencesManager.reminderVolumeFlow.first()
            val pitch = preferencesManager.speechPitchFlow.first()
            val rate = preferencesManager.speechRateFlow.first()

            val locale = getAppLocale(app.currentLanguageCode)
            val localizedContext = com.example.util.LocaleDirectionHelper.getLocalizedContext(context, app.currentLanguageCode)
            val template = localizedContext.getString(R.string.reminder_voice_template)
            val textToSpeak = String.format(locale, template, habit.name)

            speechManager.speak(
                ReminderSpeechManager.SpeechRequest(
                    text = textToSpeak,
                    locale = locale,
                    volume = volume,
                    pitch = pitch,
                    rate = rate
                )
            )
        }
    }

    /**
     * Preview voice for settings screens.
     */
    fun previewVoice(volume: Float, pitch: Float, rate: Float, langCode: String) {
        android.util.Log.d("TTS", "[TTS] Preview clicked")
        stop()
        val locale = getAppLocale(langCode)
        
        // Use localized context to ensure correct string language
        val localizedContext = com.example.util.LocaleDirectionHelper.getLocalizedContext(context, langCode)
        val textToSpeak = localizedContext.getString(R.string.reminder_voice_preview)
        
        speechManager.speak(
            ReminderSpeechManager.SpeechRequest(
                text = textToSpeak,
                locale = locale,
                volume = volume,
                pitch = pitch,
                rate = rate
            )
        )
    }

    private fun getAppLocale(langCode: String): Locale {
        return if (langCode == "system") {
            Locale.getDefault()
        } else {
            Locale.forLanguageTag(langCode)
        }
    }

    fun stop() {
        android.util.Log.d("TTS", "[TTS] stop()")
        speechManager.stopAll()
    }

    fun release() {
        android.util.Log.d("TTS", "[TTS] shutdown()")
        speechManager.release()
    }
}
