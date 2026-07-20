package com.example.core.audio

import android.content.Context
import kotlinx.coroutines.flow.Flow

interface ReminderAudioEngine {
    val type: AudioEngineType
    val isPlaying: Flow<Boolean>
    
    /**
     * Plays a reminder sound/speech.
     */
    fun play(context: Context, habitName: String, langCode: String, settings: ReminderAudioSettings)
    
    /**
     * Plays a preview sound/speech.
     */
    fun playPreview(context: Context, langCode: String, settings: ReminderAudioSettings)
    
    /**
     * Updates the volume of any currently playing audio.
     */
    fun updateVolume(volume: Float)
    
    /**
     * Stops any ongoing audio.
     */
    fun stop()
    
    /**
     * Releases resources.
     */
    fun release()
}
