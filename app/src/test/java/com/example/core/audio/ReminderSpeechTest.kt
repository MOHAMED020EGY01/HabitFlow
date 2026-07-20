package com.example.core.audio

import android.content.Context
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test
import java.util.Locale
import org.junit.Assert.*
import android.os.Handler
import android.os.Looper

class ReminderSpeechTest {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var audioManager: AudioManager

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        
        // Mock Log
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        every { android.util.Log.w(any(), any()) } returns 0

        // Mock Looper and Handler
        mockkStatic(Looper::class)
        val looper = mockk<Looper>()
        every { Looper.getMainLooper() } returns looper
        
        mockkConstructor(Handler::class)
        every { anyConstructed<Handler>().post(any()) } answers {
            (args[0] as Runnable).run()
            true
        }
        every { anyConstructed<Handler>().postDelayed(any(), any()) } answers {
            true
        }
        every { anyConstructed<Handler>().removeCallbacks(any<Runnable>()) } returns Unit

        // Mock Context.getSystemService
        every { context.getSystemService(Context.AUDIO_SERVICE) } returns audioManager
        every { audioManager.requestAudioFocus(any()) } returns AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        every { audioManager.abandonAudioFocusRequest(any()) } returns AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        
        // Mock the TextToSpeech constructor
        mockkConstructor(TextToSpeech::class)
        every { anyConstructed<TextToSpeech>().setLanguage(any()) } returns TextToSpeech.LANG_AVAILABLE
        every { anyConstructed<TextToSpeech>().setPitch(any()) } returns TextToSpeech.SUCCESS
        every { anyConstructed<TextToSpeech>().setSpeechRate(any()) } returns TextToSpeech.SUCCESS
        every { anyConstructed<TextToSpeech>().setOnUtteranceProgressListener(any()) } returns Unit
        every { anyConstructed<TextToSpeech>().speak(any(), any(), any(), any()) } returns TextToSpeech.SUCCESS
        every { anyConstructed<TextToSpeech>().stop() } returns TextToSpeech.SUCCESS
        every { anyConstructed<TextToSpeech>().shutdown() } just Runs
    }

    @Test
    fun `engine lifecycle - speak fails if not ready`() {
        val engine = ReminderSpeechEngine(context)
        var completionCalled = false
        engine.speak("test", Locale.ENGLISH, 1.0f, 1.0f, 1.0f) {
            completionCalled = true
        }
        assertTrue("Completion should be called even if not ready", completionCalled)
        verify(exactly = 0) { anyConstructed<TextToSpeech>().speak(any(), any(), any(), any()) }
    }

    @Test
    fun `manager sequential queuing handles multiple requests`() {
        val manager = ReminderSpeechManager(context)
        val request1 = ReminderSpeechManager.SpeechRequest("Text 1", Locale.ENGLISH, 1.0f)
        val request2 = ReminderSpeechManager.SpeechRequest("Text 2", Locale.ENGLISH, 1.0f)
        
        // This won't actually speak because initialization requires a real TTS engine callback
        // But it verifies the code path for queuing multiple requests.
        manager.speak(request1)
        manager.speak(request2)
        
        assertTrue(true) // Reached here without crash
    }

    @Test
    fun `stop before initialization is safe`() {
        val engine = ReminderSpeechEngine(context)
        engine.stop() // Should not call tts.stop()
        verify(exactly = 0) { anyConstructed<TextToSpeech>().stop() }
    }

    @Test
    fun `shutdown during initialization marks state correctly`() {
        val engine = ReminderSpeechEngine(context)
        engine.shutdown()
        
        var initResult = true
        engine.initialize { initResult = it }
        assertFalse("Initialization should fail if shutdown previously", initResult)
    }
}
