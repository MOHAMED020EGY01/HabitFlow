package com.example.presentation.screens.settings

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.HabitApplication
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers

data class SettingsUiState(
    val userName: String = "",
    val userPhotoUri: String = "",
    val appTheme: String = "system",
    val appLanguage: String = "system",
    val isBackgroundServiceEnabled: Boolean = false,
    val isCardAnimationsEnabled: Boolean = true,
    val glassEffectMode: Int = 0,  // 0=Auto, 1=On, 2=Off
    val isNavBarHidden: Boolean = true,
    val isSpeechEnabled: Boolean = true,
    val reminderVolume: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val speechRate: Float = 1.0f
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as HabitApplication

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        combine<Any, SettingsUiState>(
            app.preferencesManager.userNameFlow,
            app.preferencesManager.userPhotoUriFlow,
            app.preferencesManager.appThemeFlow,
            app.preferencesManager.appLanguageFlow,
            app.preferencesManager.isBackgroundServiceEnabledFlow,
            app.preferencesManager.isCardAnimationsEnabledFlow,
            app.preferencesManager.glassEffectModeFlow,
            app.preferencesManager.isNavBarHiddenFlow,
            app.preferencesManager.isSpeechEnabledFlow,
            app.preferencesManager.reminderVolumeFlow,
            app.preferencesManager.speechPitchFlow,
            app.preferencesManager.speechRateFlow
        ) { array ->
            val name = array[0] as String
            val photo = array[1] as String
            val theme = array[2] as String
            val lang = array[3] as String
            val serviceEnabled = array[4] as Boolean
            val cardAnimsEnabled = array[5] as Boolean
            val glassMode = array[6] as Int
            val navBarHidden = array[7] as Boolean
            val speechEnabled = array[8] as Boolean
            val volume = array[9] as Float
            val pitch = array[10] as Float
            val rate = array[11] as Float
            SettingsUiState(
                name, photo, theme, lang, serviceEnabled, cardAnimsEnabled, glassMode, navBarHidden,
                speechEnabled, volume, pitch, rate
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            app.preferencesManager.saveUserName(name)
        }
    }

    fun updateUserPhoto(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = app.applicationContext
                val uri = Uri.parse(uriString)
                
                // 1. Delete previous profile images from internal storage
                val filesDir = context.filesDir
                val existingFiles = filesDir.listFiles { _, name -> name.startsWith("profile_avatar_") && name.endsWith(".jpg") }
                existingFiles?.forEach { it.delete() }
                
                // 2. Decode bounds to calculate sample size
                var inputStream = context.contentResolver.openInputStream(uri)
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()
                
                // Calculate inSampleSize for a target of 512x512
                options.inSampleSize = calculateInSampleSize(options, 512, 512)
                options.inJustDecodeBounds = false
                
                // Decode bitmap
                inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                inputStream?.close()
                
                if (bitmap != null) {
                    // Resize precisely to fit 512x512 max
                    val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                    val (targetWidth, targetHeight) = if (bitmap.width > bitmap.height) {
                        512 to (512 / aspectRatio).toInt()
                    } else {
                        (512 * aspectRatio).toInt() to 512
                    }
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                    
                    if (scaledBitmap != bitmap) {
                        bitmap.recycle()
                    }
                    
                    // Save to internal storage
                    val newFile = File(filesDir, "profile_avatar_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(newFile).use { out ->
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    scaledBitmap.recycle()
                    
                    // Save the file URI in preferences
                    app.preferencesManager.saveUserPhotoUri(Uri.fromFile(newFile).toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to original uri string if anything fails
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
        viewModelScope.launch {
            app.preferencesManager.saveAppTheme(theme)
        }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch {
            app.preferencesManager.saveAppLanguage(lang)
            com.example.util.LanguageHelper.applyLanguage(lang)
            com.example.widget.HabitWidgetSyncUpdater.updateNowForced(app.applicationContext)
        }
    }

    fun setBackgroundServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            app.preferencesManager.saveBackgroundServiceEnabled(enabled)
        }
    }

    fun setCardAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            app.preferencesManager.saveCardAnimationsEnabled(enabled)
        }
    }

    fun setGlassEffectMode(mode: Int) {
        viewModelScope.launch {
            app.preferencesManager.saveGlassEffectMode(mode)
        }
    }

    fun setNavBarHidden(hidden: Boolean) {
        viewModelScope.launch {
            app.preferencesManager.saveNavBarHidden(hidden)
        }
    }

    fun setSpeechEnabled(enabled: Boolean) {
        viewModelScope.launch {
            app.preferencesManager.saveSpeechEnabled(enabled)
        }
    }

    fun setReminderVolume(volume: Float) {
        viewModelScope.launch {
            app.preferencesManager.saveReminderVolume(volume)
        }
    }

    fun setSpeechPitch(pitch: Float) {
        viewModelScope.launch {
            app.preferencesManager.saveSpeechPitch(pitch)
        }
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch {
            app.preferencesManager.saveSpeechRate(rate)
        }
    }

    fun previewVoice(volume: Float, pitch: Float, rate: Float) {
        app.reminderSpeechController.previewVoice(volume, pitch, rate, app.currentLanguageCode)
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
