package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesManager(private val context: Context) {
    companion object {
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_PHOTO_URI = stringPreferencesKey("user_photo_uri")
        val IS_ONBOARDING_COMPLETE = booleanPreferencesKey("is_onboarding_complete")
        val APP_THEME = stringPreferencesKey("app_theme")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val IS_BACKGROUND_SERVICE_ENABLED = booleanPreferencesKey("is_background_service_enabled")
        val IS_RELIABLE_BANNER_DISMISSED = booleanPreferencesKey("is_reliable_banner_dismissed")
        val IS_CARD_ANIMATIONS_ENABLED = booleanPreferencesKey("is_card_animations_enabled")
        val IS_NAV_BAR_HIDDEN = booleanPreferencesKey("is_nav_bar_hidden")
        val GLASS_EFFECT_MODE = intPreferencesKey("glass_effect_mode")
        val LAST_ROLLOVER_DATE = stringPreferencesKey("last_rollover_date")
        val IS_SPEECH_ENABLED = booleanPreferencesKey("is_speech_enabled")
        val VOICE_VOLUME = floatPreferencesKey("voice_volume")
        val RINGTONE_VOLUME = floatPreferencesKey("ringtone_volume")
        val SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val SELECTED_AUDIO_ENGINE = stringPreferencesKey("selected_audio_engine")
        val ALARM_URI = stringPreferencesKey("alarm_uri")
        val ALARM_DURATION = intPreferencesKey("alarm_duration")
        val TTS_REPEATS = intPreferencesKey("tts_repeats")
    }

    val userNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME] ?: ""
    }

    val userPhotoUriFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_PHOTO_URI] ?: ""
    }

    val isOnboardingCompleteFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_ONBOARDING_COMPLETE] ?: false
    }

    val appThemeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[APP_THEME] ?: "system"
    }

    val appLanguageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[APP_LANGUAGE] ?: "system"
    }

    val isBackgroundServiceEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_BACKGROUND_SERVICE_ENABLED] ?: false
    }

    val isReliableBannerDismissedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_RELIABLE_BANNER_DISMISSED] ?: false
    }

    val isCardAnimationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_CARD_ANIMATIONS_ENABLED] ?: true
    }

    val isNavBarHiddenFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_NAV_BAR_HIDDEN] ?: true
    }

    val glassEffectModeFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[GLASS_EFFECT_MODE] ?: 0
    }

    val lastRolloverDateFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LAST_ROLLOVER_DATE] ?: ""
    }

    val isSpeechEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_SPEECH_ENABLED] ?: true
    }

    val voiceVolumeFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[VOICE_VOLUME] ?: 1.0f
    }

    val ringtoneVolumeFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[RINGTONE_VOLUME] ?: 1.0f
    }

    val speechPitchFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[SPEECH_PITCH] ?: 1.0f
    }

    val speechRateFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[SPEECH_RATE] ?: 1.0f
    }

    val selectedAudioEngineFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_AUDIO_ENGINE] ?: "ALARM"
    }

    val alarmUriFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ALARM_URI] ?: ""
    }

    val alarmDurationFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[ALARM_DURATION] ?: 30
    }

    val ttsRepeatsFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[TTS_REPEATS] ?: 1
    }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = name
        }
    }

    suspend fun saveUserPhotoUri(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_PHOTO_URI] = uri
        }
    }

    suspend fun saveOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_ONBOARDING_COMPLETE] = complete
        }
    }

    suspend fun saveAppTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[APP_THEME] = theme
        }
    }

    suspend fun saveAppLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[APP_LANGUAGE] = lang
            com.example.util.LanguageHelper.applyLanguage(lang)
        }
    }

    suspend fun saveBackgroundServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_BACKGROUND_SERVICE_ENABLED] = enabled
        }
    }

    suspend fun saveReliableBannerDismissed(dismissed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_RELIABLE_BANNER_DISMISSED] = dismissed
        }
    }

    suspend fun saveCardAnimationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_CARD_ANIMATIONS_ENABLED] = enabled
        }
    }

    suspend fun saveNavBarHidden(hidden: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_NAV_BAR_HIDDEN] = hidden
        }
    }

    suspend fun saveGlassEffectMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[GLASS_EFFECT_MODE] = mode
        }
    }

    suspend fun saveLastRolloverDate(date: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_ROLLOVER_DATE] = date
        }
    }

    suspend fun saveSpeechEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_SPEECH_ENABLED] = enabled
        }
    }

    suspend fun saveVoiceVolume(volume: Float) {
        context.dataStore.edit { preferences ->
            preferences[VOICE_VOLUME] = volume
        }
    }

    suspend fun saveRingtoneVolume(volume: Float) {
        context.dataStore.edit { preferences ->
            preferences[RINGTONE_VOLUME] = volume
        }
    }

    suspend fun saveSpeechPitch(pitch: Float) {
        context.dataStore.edit { preferences ->
            preferences[SPEECH_PITCH] = pitch
        }
    }

    suspend fun saveSpeechRate(rate: Float) {
        context.dataStore.edit { preferences ->
            preferences[SPEECH_RATE] = rate
        }
    }

    suspend fun saveSelectedAudioEngine(engine: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_AUDIO_ENGINE] = engine
        }
    }

    suspend fun saveAlarmUri(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[ALARM_URI] = uri
        }
    }

    suspend fun saveAlarmDuration(duration: Int) {
        context.dataStore.edit { preferences ->
            preferences[ALARM_DURATION] = duration
        }
    }

    suspend fun saveTtsRepeats(repeats: Int) {
        context.dataStore.edit { preferences ->
            preferences[TTS_REPEATS] = repeats
        }
    }

    suspend fun clearAllData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
