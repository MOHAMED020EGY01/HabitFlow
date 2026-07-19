package com.example.domain.audio

import android.content.Context

interface ReminderAudioEngine {
    val type: AudioEngineType
    
    /**
     * Plays a reminder sound/speech.
     */
    fun play(context: Context, habitName: String, langCode: String, settings: ReminderAudioSettings)
    
    /**
     * Plays a preview sound/speech.
     */
    fun playPreview(context: Context, langCode: String, settings: ReminderAudioSettings)
    
    /**
     * Stops any ongoing audio.
     */
    fun stop()
    
    /**
     * Releases resources.
     */
    fun release()
}
