package com.example.speech

import android.content.Context
import java.util.Locale
import java.util.LinkedList
import java.util.Queue

/**
 * Orchestrates speech requests, handles queueing and lifecycle.
 */
class ReminderSpeechManager(private val context: Context) {

    private var engine: ReminderSpeechEngine? = null
    private val speechQueue: Queue<SpeechRequest> = LinkedList()
    private var isProcessing = false
    private var stopRequested = false
    
    // To track and cancel pending async tasks
    private var currentTaskToken: Long = 0

    data class SpeechRequest(
        val text: String,
        val locale: Locale,
        val voiceVolume: Float,
        val pitch: Float = 1.0f,
        val rate: Float = 1.0f,
        val onDone: (() -> Unit)? = null
    )

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val idleReleaseRunnable = Runnable { release() }
    private val IDLE_TIMEOUT_MS = 60_000L // 1 minute idle timeout

    fun speak(request: SpeechRequest) {
        android.util.Log.d("TTS", "[TTS] speak() requested. Text: ${request.text}")
        handler.removeCallbacks(idleReleaseRunnable)
        synchronized(this) {
            stopRequested = false
            speechQueue.add(request)
        }
        processQueue()
    }

    private fun processQueue() {
        synchronized(this) {
            if (isProcessing || speechQueue.isEmpty() || stopRequested) {
                if (speechQueue.isEmpty() && !isProcessing) {
                    scheduleIdleRelease()
                }
                return
            }
            isProcessing = true
            currentTaskToken++
        }
        
        val taskToken = currentTaskToken
        handler.removeCallbacks(idleReleaseRunnable)

        val request = synchronized(this) {
            if (stopRequested) null else speechQueue.poll()
        }

        if (request == null) {
            synchronized(this) { isProcessing = false }
            scheduleIdleRelease()
            return
        }

        ensureEngine { ready ->
            val shouldSpeak: Boolean
            synchronized(this) {
                // Ensure we are still processing the SAME task and stop wasn't requested
                shouldSpeak = ready && !stopRequested && taskToken == currentTaskToken
            }

            if (shouldSpeak) {
                engine?.speak(
                    request.text,
                    request.locale,
                    request.voiceVolume,
                    request.pitch,
                    request.rate
                ) {
                    onTaskDone(request, taskToken)
                }
            } else {
                onTaskDone(request, taskToken)
            }
        }
    }

    private fun onTaskDone(request: SpeechRequest, taskToken: Long) {
        synchronized(this) {
            // Always set isProcessing to false if this was the task we were waiting for,
            // even if the token changed, so we don't block the queue forever.
            isProcessing = false
            
            if (taskToken == currentTaskToken) {
                try {
                    request.onDone?.invoke()
                } catch (e: Exception) {
                    android.util.Log.e("TTS", "[TTS] Error in onDone callback", e)
                }
            } else {
                android.util.Log.d("TTS", "[TTS] Task discarded (token mismatch)")
            }
            processQueue()
        }
    }

    private fun scheduleIdleRelease() {
        handler.removeCallbacks(idleReleaseRunnable)
        handler.postDelayed(idleReleaseRunnable, IDLE_TIMEOUT_MS)
    }

    private fun ensureEngine(onReady: (Boolean) -> Unit) {
        synchronized(this) {
            if (engine == null) {
                engine = ReminderSpeechEngine(context)
            }
        }
        engine?.initialize(onReady)
    }

    fun stopAll() {
        android.util.Log.d("TTS", "[TTS] stopAll()")
        synchronized(this) {
            stopRequested = true
            currentTaskToken++ // Cancel any pending async callback
            engine?.stop()
            speechQueue.clear()
        }
        scheduleIdleRelease()
    }

    fun release() {
        android.util.Log.d("TTS", "[TTS] release() / shutdown()")
        handler.removeCallbacks(idleReleaseRunnable)
        synchronized(this) {
            stopRequested = true
            currentTaskToken++
            engine?.shutdown()
            engine = null
            speechQueue.clear()
            isProcessing = false
        }
    }
}
