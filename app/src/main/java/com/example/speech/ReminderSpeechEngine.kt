package com.example.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Low-level wrapper for TextToSpeech and Audio Focus management.
 */
class ReminderSpeechEngine(private val context: Context) {

    enum class EngineState {
        NOT_INITIALIZED,
        INITIALIZING,
        READY,
        SHUTDOWN
    }

    private var tts: TextToSpeech? = null
    private var state = EngineState.NOT_INITIALIZED
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null

    private val initListeners = mutableListOf<(Boolean) -> Unit>()

    fun initialize(onReady: (Boolean) -> Unit) {
        val appContext = context.applicationContext
        android.util.Log.d("TTS", "[TTS] initialize() called. Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}, OS: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        android.util.Log.d("TTS", "[TTS] Current Locale: ${java.util.Locale.getDefault()}")
        
        synchronized(this) {
            when (state) {
                EngineState.READY -> {
                    android.util.Log.d("TTS", "[TTS] Engine already READY")
                    onReady(true)
                    return
                }
                EngineState.INITIALIZING -> {
                    android.util.Log.d("TTS", "[TTS] Engine currently INITIALIZING, adding listener")
                    initListeners.add(onReady)
                    return
                }
                EngineState.SHUTDOWN -> {
                    android.util.Log.d("TTS", "[TTS] Engine is SHUTDOWN, returning false")
                    onReady(false)
                    return
                }
                EngineState.NOT_INITIALIZED -> {
                    state = EngineState.INITIALIZING
                    initListeners.add(onReady)
                    tts?.shutdown()
                }
            }
        }

        // 1. Verify we are on the Main Thread
        val isMainThread = android.os.Looper.myLooper() == android.os.Looper.getMainLooper()
        android.util.Log.d("TTS", "[TTS] Created on Main Thread: $isMainThread")

        // 2. Log installed engines and default engine
        val engines = try {
            val dummyTts = TextToSpeech(appContext, null)
            val result = dummyTts.engines
            dummyTts.shutdown()
            result
        } catch (e: Exception) {
            android.util.Log.e("TTS", "[TTS] Failed to query engines", e)
            emptyList<TextToSpeech.EngineInfo>()
        }
        
        android.util.Log.d("TTS", "[TTS] Found ${engines.size} TTS engines in PackageManager")
        engines.forEach { engineInfo ->
            val isEnabled = isPackageEnabled(engineInfo.name)
            android.util.Log.d("TTS", "[TTS] Engine: ${engineInfo.name} (label: ${engineInfo.label}), Enabled: $isEnabled")
        }

        if (engines.isEmpty()) {
            android.util.Log.e("TTS", "[TTS] FATAL: No TTS engines installed on this device!")
        } else if (engines.none { isPackageEnabled(it.name) }) {
            android.util.Log.e("TTS", "[TTS] FATAL: TTS engines are installed but ALL ARE DISABLED by the user or system.")
        }

        // 3. Create real TextToSpeech instance using Application Context
        android.util.Log.d("TTS", "[TTS] Creating TextToSpeech using Application Context")
        
        // Find Google engine as preferred fallback if it's enabled
        val googleEngine = engines.find { it.name == "com.google.android.tts" && isPackageEnabled(it.name) }?.name
        
        tts = if (googleEngine != null) {
            android.util.Log.d("TTS", "[TTS] Using Google TTS engine explicitly")
            TextToSpeech(appContext, { status -> handleInit(status) }, googleEngine)
        } else {
            android.util.Log.d("TTS", "[TTS] Using system default engine")
            TextToSpeech(appContext) { status -> handleInit(status) }
        }

        // Safety net: If TTS constructor fails to trigger onInit (rare but possible on some OS versions)
        handler.postDelayed({
            synchronized(this) {
                if (state == EngineState.INITIALIZING) {
                    android.util.Log.e("TTS", "[TTS] Init timeout reached. Forcing failure state.")
                    handleInit(TextToSpeech.ERROR)
                }
            }
        }, 5000)
    }

    private fun isPackageEnabled(packageName: String): Boolean {
        return try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            info.enabled
        } catch (_: Exception) {
            false
        }
    }

    private fun handleInit(status: Int) {
        val success = status == TextToSpeech.SUCCESS
        val currentEngine = tts?.defaultEngine
        android.util.Log.d("TTS", "[TTS] onInit callback. Status: ${if (success) "SUCCESS" else "FAILED ($status)"}. Current Engine: $currentEngine")
        
        if (!success) {
            android.util.Log.e("TTS", "[TTS] FATAL: Initialization failed with code $status.")
        }

        val listeners: List<(Boolean) -> Unit>
        synchronized(this) {
            if (state == EngineState.SHUTDOWN) {
                android.util.Log.d("TTS", "[TTS] shutdown() called while initializing")
                tts?.shutdown()
                tts = null
                listeners = initListeners.toList()
                initListeners.clear()
            } else {
                state = if (success) EngineState.READY else EngineState.NOT_INITIALIZED
                listeners = initListeners.toList()
                initListeners.clear()
            }
        }
        
        handler.post {
            listeners.forEach { it(success && state == EngineState.READY) }
        }
    }

    fun speak(
        text: String,
        locale: Locale,
        appVolume: Float,
        pitch: Float,
        rate: Float,
        onCompletion: () -> Unit
    ) {
        android.util.Log.d("TTS", "[TTS] speak() requested: '$text'")
        synchronized(this) {
            if (state != EngineState.READY || tts == null) {
                android.util.Log.e("TTS", "[TTS] speak() failed: Engine not READY (state=$state)")
                onCompletion()
                return
            }
        }

        try {
            android.util.Log.d("TTS", "[TTS] Language = ${locale.language}")
            var result = tts?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
            
            // Fallback to base language if specific country/variant fails
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                android.util.Log.w("TTS", "[TTS] Language $locale not supported, trying base language: ${locale.language}")
                @Suppress("DEPRECATION")
                result = tts?.setLanguage(Locale(locale.language)) ?: TextToSpeech.LANG_NOT_SUPPORTED
            }

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                android.util.Log.e("TTS", "[TTS] Language ${locale.language} is not supported or missing data (result code: $result).")
                onCompletion()
                return
            }

            tts?.apply {
                setPitch(pitch)
                setSpeechRate(rate)
                
                // Set audio attributes explicitly for Alarm Stream to match AlarmSoundEngine
                val attr = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                setAudioAttributes(attr)
            }

            val params = Bundle().apply {
                // Use STREAM_ALARM for legacy support
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, appVolume)
            }

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    android.util.Log.d("TTS", "[TTS] onStart($utteranceId)")
                }
                override fun onDone(utteranceId: String?) {
                    android.util.Log.d("TTS", "[TTS] onDone($utteranceId)")
                    cleanup()
                    handler.post { onCompletion() }
                }
                override fun onError(utteranceId: String?) {
                    android.util.Log.e("TTS", "[TTS] onError($utteranceId)")
                    cleanup()
                    handler.post { onCompletion() }
                }
                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    android.util.Log.d("TTS", "[TTS] onStop($utteranceId, interrupted=$interrupted)")
                    cleanup()
                    handler.post { onCompletion() }
                }
            })

            android.util.Log.d("TTS", "[TTS] Audio Focus Requesting")
            requestFocus()
            
            val utteranceId = "HABIT_REMINDER_${System.currentTimeMillis()}"
            android.util.Log.d("TTS", "[TTS] Calling speak()")
            val speakResult = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            android.util.Log.d("TTS", "[TTS] speak() result = ${if (speakResult == TextToSpeech.SUCCESS) "SUCCESS" else "ERROR ($speakResult)"}")
            
            if (speakResult == TextToSpeech.ERROR) {
                cleanup()
                onCompletion()
                return
            }

            // Set 30s timeout safety net
            stopRunnable?.let { handler.removeCallbacks(it) }
            stopRunnable = Runnable {
                android.util.Log.w("TTS", "[TTS] 30s timeout reached, forcing stop")
                stop()
                onCompletion()
            }
            handler.postDelayed(stopRunnable!!, 30_000)
        } catch (e: Exception) {
            android.util.Log.e("TTS", "[TTS] Crash during speak()", e)
            cleanup()
            onCompletion()
        }
    }

    private fun requestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attr)
                .build()
            val res = audioManager.requestAudioFocus(audioFocusRequest!!)
            android.util.Log.d("TTS", "[TTS] Audio Focus ${if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) "GRANTED" else "DENIED ($res)"}")
        } else {
            @Suppress("DEPRECATION")
            val res = audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            android.util.Log.d("TTS", "[TTS] Audio Focus (Legacy) ${if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) "GRANTED" else "DENIED ($res)"}")
        }
    }

    private fun abandonFocus() {
        android.util.Log.d("TTS", "[TTS] abandonFocus()")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun cleanup() {
        stopRunnable?.let { handler.removeCallbacks(it) }
        abandonFocus()
    }

    fun stop() {
        android.util.Log.d("TTS", "[TTS] stop()")
        cleanup()
        synchronized(this) {
            if (state == EngineState.READY) {
                tts?.stop()
            }
        }
    }

    fun shutdown() {
        android.util.Log.d("TTS", "[TTS] shutdown()")
        cleanup()
        synchronized(this) {
            if (state == EngineState.READY) {
                tts?.stop()
                tts?.shutdown()
                tts = null
                state = EngineState.SHUTDOWN
            } else if (state == EngineState.INITIALIZING) {
                state = EngineState.SHUTDOWN
            } else {
                state = EngineState.SHUTDOWN
                tts = null
            }
        }
    }
}
