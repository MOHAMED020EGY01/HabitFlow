package com.example.presentation.screens.settings

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HabitApplication
import com.example.domain.audio.AudioEngineType
import com.example.domain.audio.ReminderAudioSettings
import com.example.domain.audio.TTSStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class SettingsUiState(
    val userName: String = "",
    val userPhotoUri: String = "",
    val appTheme: String = "system",
    val appLanguage: String = "system",
    val isBackgroundServiceEnabled: Boolean = false,
    val isCardAnimationsEnabled: Boolean = true,
    val glassEffectMode: Int = 0,
    val isNavBarHidden: Boolean = true,
    val audioSettings: ReminderAudioSettings = ReminderAudioSettings(),
    val ttsStatus: TTSStatus = TTSStatus.INITIALIZING
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as HabitApplication
    private val audioRepository = app.reminderAudioRepository

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        combine(
            app.preferencesManager.userNameFlow,
            app.preferencesManager.userPhotoUriFlow,
            app.preferencesManager.appThemeFlow,
            app.preferencesManager.appLanguageFlow,
            app.preferencesManager.isBackgroundServiceEnabledFlow,
            app.preferencesManager.isCardAnimationsEnabledFlow,
            app.preferencesManager.glassEffectModeFlow,
            app.preferencesManager.isNavBarHiddenFlow,
            audioRepository.settings,
            audioRepository.ttsStatus
        ) { array ->
            SettingsUiState(
                userName = array[0] as String,
                userPhotoUri = array[1] as String,
                appTheme = array[2] as String,
                appLanguage = array[3] as String,
                isBackgroundServiceEnabled = array[4] as Boolean,
                isCardAnimationsEnabled = array[5] as Boolean,
                glassEffectMode = array[6] as Int,
                isNavBarHidden = array[7] as Boolean,
                audioSettings = array[8] as ReminderAudioSettings,
                ttsStatus = array[9] as TTSStatus
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun updateUserName(name: String) {
        viewModelScope.launch { app.preferencesManager.saveUserName(name) }
    }

    fun updateUserPhoto(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = app.applicationContext
                val uri = Uri.parse(uriString)
                val filesDir = context.filesDir
                filesDir.listFiles { _, name -> name.startsWith("profile_avatar_") && name.endsWith(".jpg") }?.forEach { it.delete() }
                
                var inputStream = context.contentResolver.openInputStream(uri)
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()
                
                options.inSampleSize = calculateInSampleSize(options, 512, 512)
                options.inJustDecodeBounds = false
                
                inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()
                
                if (bitmap != null) {
                    val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val (targetWidth, targetHeight) = if (bitmap.width > bitmap.height) {
                        512 to (512 / aspectRatio).toInt()
                    } else {
                        (512 * aspectRatio).toInt() to 512
                    }
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                    if (scaledBitmap != bitmap) bitmap.recycle()
                    
                    val newFile = File(filesDir, "profile_avatar_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(newFile).use { out -> scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out) }
                    scaledBitmap.recycle()
                    app.preferencesManager.saveUserPhotoUri(Uri.fromFile(newFile).toString())
                }
            } catch (e: Exception) {
                app.preferencesManager.saveUserPhotoUri(uriString)
            }
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun updateTheme(theme: String) {
        viewModelScope.launch { app.preferencesManager.saveAppTheme(theme) }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch {
            app.preferencesManager.saveAppLanguage(lang)
            com.example.util.LanguageHelper.applyLanguage(lang)
            com.example.widget.HabitWidgetSyncUpdater.updateNowForced(app.applicationContext)
        }
    }

    fun setBackgroundServiceEnabled(enabled: Boolean) {
        viewModelScope.launch { app.preferencesManager.saveBackgroundServiceEnabled(enabled) }
    }

    fun setCardAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch { app.preferencesManager.saveCardAnimationsEnabled(enabled) }
    }

    fun setGlassEffectMode(mode: Int) {
        viewModelScope.launch { app.preferencesManager.saveGlassEffectMode(mode) }
    }

    fun setNavBarHidden(hidden: Boolean) {
        viewModelScope.launch { app.preferencesManager.saveNavBarHidden(hidden) }
    }

    // Audio Methods
    fun setAudioEngine(type: AudioEngineType) {
        viewModelScope.launch {
            audioRepository.updateSettings(uiState.value.audioSettings.copy(selectedEngine = type))
        }
    }

    fun setAlarmUri(uri: String) {
        viewModelScope.launch {
            audioRepository.updateSettings(uiState.value.audioSettings.copy(alarmUri = uri))
        }
    }

    fun setAlarmDuration(seconds: Int) {
        viewModelScope.launch {
            audioRepository.updateSettings(uiState.value.audioSettings.copy(alarmDurationSeconds = seconds))
        }
    }

    fun setTtsRepeats(repeats: Int) {
        viewModelScope.launch {
            audioRepository.updateSettings(uiState.value.audioSettings.copy(ttsRepeats = repeats))
        }
    }

    fun setReminderVolume(volume: Float) {
        viewModelScope.launch {
            audioRepository.updateSettings(uiState.value.audioSettings.copy(volume = volume))
        }
    }

    fun setSpeechPitch(pitch: Float) {
        viewModelScope.launch {
            audioRepository.updateSettings(uiState.value.audioSettings.copy(pitch = pitch))
        }
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch {
            audioRepository.updateSettings(uiState.value.audioSettings.copy(rate = rate))
        }
    }

    fun testReminder() {
        Log.d("AUDIO", "[AUDIO] Dynamic Preview Button Clicked")
        audioRepository.stop()
        audioRepository.playPreview()
    }

    fun verifyTtsStatus() {
        viewModelScope.launch {
            audioRepository.verifyTTS()
        }
    }

    fun resetAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            app.preferencesManager.clearAllData()
            val db = com.example.data.local.database.HabitDatabase.getDatabase(app)
            db.clearAllTables()
            com.example.widget.HabitWidgetSyncUpdater.updateNowForced(app.applicationContext)
            onComplete()
        }
    }
}
