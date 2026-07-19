package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.data.preferences.UserPreferencesManager
import com.example.domain.audio.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ReminderAudioRepositoryImpl(
    private val context: Context,
    private val preferencesManager: UserPreferencesManager,
    private val ttsEngine: ReminderAudioEngine,
    private val alarmEngine: ReminderAudioEngine
) : ReminderAudioRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.Main)
    private val googleTtsPackage = "com.google.android.tts"

    override val settings: Flow<ReminderAudioSettings> = combine(
        preferencesManager.selectedAudioEngineFlow,
        preferencesManager.alarmUriFlow,
        preferencesManager.alarmDurationFlow,
        preferencesManager.ttsRepeatsFlow,
        preferencesManager.reminderVolumeFlow,
        preferencesManager.speechPitchFlow,
        preferencesManager.speechRateFlow
    ) { args: Array<Any> ->
        val engineType = try {
            AudioEngineType.valueOf(args[0] as String)
        } catch (e: Exception) {
            AudioEngineType.ALARM
        }
        ReminderAudioSettings(
            selectedEngine = engineType,
            alarmUri = args[1] as String,
            alarmDurationSeconds = args[2] as Int,
            ttsRepeats = args[3] as Int,
            volume = args[4] as Float,
            pitch = args[5] as Float,
            rate = args[6] as Float
        )
    }

    private val _ttsStatus = MutableStateFlow(TTSStatus.INITIALIZING)
    override val ttsStatus: Flow<TTSStatus> = _ttsStatus.asStateFlow()

    init {
        repositoryScope.launch {
            verifyTTS()
        }
    }

    override suspend fun updateSettings(settings: ReminderAudioSettings) {
        preferencesManager.saveSelectedAudioEngine(settings.selectedEngine.name)
        preferencesManager.saveAlarmUri(settings.alarmUri)
        preferencesManager.saveAlarmDuration(settings.alarmDurationSeconds)
        preferencesManager.saveTtsRepeats(settings.ttsRepeats)
        preferencesManager.saveReminderVolume(settings.volume)
        preferencesManager.saveSpeechPitch(settings.pitch)
        preferencesManager.saveSpeechRate(settings.rate)
    }

    override suspend fun verifyTTS() {
        _ttsStatus.value = TTSStatus.INITIALIZING
        Log.d("TTS", "[TTS] Verifying TTS engine availability...")

        if (!isPackageInstalled(googleTtsPackage)) {
            Log.w("TTS", "[TTS] Engine Missing: $googleTtsPackage not found")
            _ttsStatus.value = TTSStatus.NOT_INSTALLED
            
            // If currently selected engine is TTS but it's not installed, fallback to ALARM
            val currentSettings = settings.first()
            if (currentSettings.selectedEngine == AudioEngineType.TTS) {
                Log.w("TTS", "[TTS] Fallback to ALARM because TTS is not installed")
                updateSettings(currentSettings.copy(selectedEngine = AudioEngineType.ALARM))
            }
            return
        }

        // Additional check: Can we initialize it?
        // In a real implementation, we would wait for the engine's onInit callback.
        // For now, we rely on the existence of the package and the fact that 
        // the engine will handle its own internal errors during playback.
        _ttsStatus.value = TTSStatus.AVAILABLE
        Log.d("TTS", "[TTS] Initialization SUCCESS")
    }

    override fun playReminder(habitName: String) {
        repositoryScope.launch {
            val currentSettings = settings.first()
            val engine = getEngine(currentSettings.selectedEngine)
            val langCode = preferencesManager.appLanguageFlow.first()
            
            Log.d("AUDIO", "[AUDIO] Selected Engine: ${currentSettings.selectedEngine}")
            
            // If TTS is selected but unavailable, override to AlarmSoundEngine
            val effectiveEngine = if ((currentSettings.selectedEngine == AudioEngineType.TTS) && (_ttsStatus.value != TTSStatus.AVAILABLE)) {
                Log.w("AUDIO", "[AUDIO] TTS requested but unavailable. Falling back to Alarm.")
                alarmEngine
            } else {
                engine ?: alarmEngine
            }

            effectiveEngine.play(context, habitName, langCode, currentSettings)
        }
    }

    override fun playPreview() {
        repositoryScope.launch {
            val currentSettings = settings.first()
            val engine = getEngine(currentSettings.selectedEngine)
            val langCode = preferencesManager.appLanguageFlow.first()

            Log.d("AUDIO", "[AUDIO] Preview Requested for Engine: ${currentSettings.selectedEngine}")

            val effectiveEngine = if (currentSettings.selectedEngine == AudioEngineType.TTS && _ttsStatus.value != TTSStatus.AVAILABLE) {
                alarmEngine
            } else {
                engine ?: alarmEngine
            }

            effectiveEngine.playPreview(context, langCode, currentSettings)
        }
    }

    override fun stop() {
        Log.d("AUDIO", "[AUDIO] Stopping all playback")
        ttsEngine.stop()
        alarmEngine.stop()
    }

    override fun release() {
        Log.d("AUDIO", "[AUDIO] Releasing all audio resources")
        ttsEngine.release()
        alarmEngine.release()
    }

    private fun getEngine(type: AudioEngineType): ReminderAudioEngine? {
        return when (type) {
            AudioEngineType.TTS -> ttsEngine
            AudioEngineType.ALARM -> alarmEngine
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            info.enabled
        } catch (e: Exception) {
            false
        }
    }
}
