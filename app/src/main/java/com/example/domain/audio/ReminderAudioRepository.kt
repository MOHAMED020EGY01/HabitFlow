package com.example.domain.audio

import kotlinx.coroutines.flow.Flow

interface ReminderAudioRepository {
    val settings: Flow<ReminderAudioSettings>
    val ttsStatus: Flow<TTSStatus>
    val isPlaying: Flow<Boolean>
    
    suspend fun updateSettings(settings: ReminderAudioSettings)
    
    /**
     * Re-verifies TTS availability on the device.
     */
    suspend fun verifyTTS()
    
    /**
     * Trigger a reminder playback based on current settings.
     */
    fun playReminder(habitName: String)
    
    /**
     * Trigger a preview playback based on current settings.
     */
    fun playPreview()
    
    /**
     * Stops any ongoing playback.
     */
    fun stop()
    
    /**
     * Releases all audio resources.
     */
    fun release()
}
