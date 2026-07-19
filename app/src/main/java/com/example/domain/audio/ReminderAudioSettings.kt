package com.example.domain.audio

data class ReminderAudioSettings(
    val selectedEngine: AudioEngineType = AudioEngineType.ALARM,
    val alarmUri: String = "", // Empty means system default alarm
    val alarmDurationSeconds: Int = 30, // 15, 30, 60, -1 (until dismissed)
    val ttsRepeats: Int = 1, // 1, 2
    val voiceVolume: Float = 1.0f,
    val ringtoneVolume: Float = 1.0f,
    val pitch: Float = 1.0f,
    val rate: Float = 1.0f
)
