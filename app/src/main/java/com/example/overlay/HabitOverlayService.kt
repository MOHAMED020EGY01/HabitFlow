package com.example.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.HabitApplication
import com.example.R
import com.example.overlay.composable.HabitOverlayContent
import kotlinx.coroutines.*
import java.time.LocalDate
import kotlin.math.abs

class HabitOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var isOverlayAdded = false
    private var lifecycleOwner: ServiceLifecycleOwner? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var mediaPlayer: MediaPlayer? = null
    private var soundStopJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val habitId    = intent?.getIntExtra("HABIT_ID", -1) ?: -1
        val habitName  = intent?.getStringExtra("HABIT_NAME")  ?: applicationContext.getString(com.example.R.string.channel_habit_reminders)
        val habitColor = intent?.getStringExtra("HABIT_COLOR") ?: "#7C4DFF"
        val habitDesc  = intent?.getStringExtra("HABIT_DESC")  ?: ""

        android.util.Log.e("ReminderChain", "[HabitOverlayService] onStartCommand for '$habitName'")
        
        try {
            startForegroundWithNotification(habitName)
        } catch (e: Exception) {
            android.util.Log.e("ReminderChain", "[HabitOverlayService] CRITICAL: startForegroundWithNotification failed", e)
        }
        
        playOverlaySound()
        showOverlay(habitId, habitName, habitColor, habitDesc)

        return START_NOT_STICKY
    }

    private fun playOverlaySound() {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: return
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, notificationUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }

            // Stop sound after 20 seconds
            soundStopJob?.cancel()
            soundStopJob = serviceScope.launch {
                delay(20_000L)
                stopAndReleaseMediaPlayer()
            }
        } catch (e: Exception) {
            Log.e("HabitOverlay", "Failed to play overlay sound", e)
        }
    }

    private fun stopAndReleaseMediaPlayer() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("HabitOverlay", "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
            soundStopJob?.cancel()
            soundStopJob = null
        }
    }

    private fun startForegroundWithNotification(habitName: String) {
        val app = applicationContext as HabitApplication
        val langCode = app.currentLanguageCode
        val localizedContext = com.example.util.LocaleDirectionHelper.getLocalizedContext(applicationContext, langCode)

        val channelId = "habit_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                applicationContext.getString(com.example.R.string.channel_overlay),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = applicationContext.getString(com.example.R.string.channel_overlay) }

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_habit_notification)
            .setContentTitle(localizedContext.getString(com.example.R.string.overlay_title))
            .setContentText(localizedContext.getString(com.example.R.string.notification_time_for_habit, habitName))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        try {
            android.util.Log.e("ReminderChain", "[HabitOverlayService] calling startForeground(1001, ...)")
            startForeground(1001, notification)
            android.util.Log.e("ReminderChain", "[HabitOverlayService] startForeground call completed successfully.")
        } catch (e: Exception) {
            android.util.Log.e("ReminderChain", "[HabitOverlayService] startForeground FAILED", e)
            throw e
        }
    }

    private fun showOverlay(
        habitId: Int,
        habitName: String,
        habitColor: String,
        habitDesc: String
    ) {
        android.util.Log.e("HabitOverlay", "[STATE] showOverlay attempt for '$habitName'. isOverlayAdded: $isOverlayAdded")
        if (isOverlayAdded) {
            android.util.Log.w("HabitOverlay", "[STATE] Overwriting existing overlay view")
            dismissOverlayViewOnly()
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = 0
        }

        val activeLifecycleOwner = ServiceLifecycleOwner().apply {
            start()
            resume()
        }
        lifecycleOwner = activeLifecycleOwner

        val app = applicationContext as HabitApplication
        val langCode = app.currentLanguageCode
        val localizedContext = com.example.util.LocaleDirectionHelper.getLocalizedContext(applicationContext, langCode)
        
        // Get a random quote for the overlay
        val quoteProvider = com.example.util.MotivationalQuoteProvider(localizedContext)
        val randomQuote = quoteProvider.getRandomQuote()

        val composeView = ComposeView(localizedContext).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(activeLifecycleOwner)
            setViewTreeViewModelStoreOwner(activeLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(activeLifecycleOwner)
            setContent {
                val direction = if (com.example.util.LocaleDirectionHelper.isRtl(langCode)) {
                    androidx.compose.ui.unit.LayoutDirection.Rtl
                } else {
                    androidx.compose.ui.unit.LayoutDirection.Ltr
                }
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides direction
                ) {
                    com.example.ui.theme.HabitFlowTheme(darkTheme = true) {
                        HabitOverlayContent(
                            habitName  = habitName,
                            habitColor = habitColor,
                            habitDesc  = habitDesc,
                            quote      = randomQuote,
                            onDone = {
                                stopAndReleaseMediaPlayer()
                                dismissOverlayViewOnly()
                                serviceScope.launch {
                                    val today = LocalDate.now().toString() // "yyyy-MM-dd"
                                    android.util.Log.d("HabitWidgetPerf", "[LOG] HabitOverlayService: Log completion started. Timestamp: ${System.currentTimeMillis()}")
                                    app.repository.logHabitCompletion(
                                        habitId   = habitId,
                                        date      = today,
                                        completed = true
                                    )
                                    android.util.Log.d("HabitWidgetPerf", "[LOG] HabitOverlayService: DB write completed. Timestamp: ${System.currentTimeMillis()}")
                                    
                                    com.example.widget.HabitWidgetSyncUpdater.updateNowForced(applicationContext)
                                    OverlayQueueManager.getInstance(applicationContext).onOverlayDismissed()
                                    stopSelf()
                                }
                            },
                            onDismiss = {
                                stopAndReleaseMediaPlayer()
                                dismissOverlay()
                            }
                        )
                    }
                }
            }
        }

        // Drag support — lets the user reposition the card freely
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        composeView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) isDragging = true
                    if (isDragging) {
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try {
                            windowManager.updateViewLayout(composeView, params)
                        } catch (e: Exception) {
                            Log.e("HabitOverlay", "Failed to update overlay layout", e)
                        }
                    }
                    true
                }
                else -> false
            }
        }

        try {
            android.util.Log.e("ReminderChain", "[HabitOverlayService] showOverlay: adding view to windowManager")
            windowManager.addView(composeView, params)
            overlayView = composeView
            isOverlayAdded = true
            android.util.Log.e("ReminderChain", "[HabitOverlayService] showOverlay: view added successfully")
        } catch (e: Exception) {
            Log.e("ReminderChain", "[HabitOverlayService] showOverlay: Failed to add overlay view", e)
            activeLifecycleOwner.stop()
            lifecycleOwner = null
            stopSelf()
        }
    }

    private fun dismissOverlayViewOnly() {
        android.util.Log.e("HabitOverlay", "[STATE] dismissOverlayViewOnly. isOverlayAdded: $isOverlayAdded")
        if (!isOverlayAdded) return
        try {
            (overlayView as? ComposeView)?.disposeComposition()
            overlayView?.let { windowManager.removeView(it) }
            android.util.Log.e("HabitOverlay", "[STATE] View successfully removed from WindowManager")
        } catch (e: Exception) {
            Log.w("HabitOverlay", "[STATE] Error removing view from WindowManager", e)
        } finally {
            overlayView = null
            isOverlayAdded = false
            lifecycleOwner?.stop()
            lifecycleOwner = null
            android.util.Log.e("HabitOverlay", "[STATE] Local state reset: isOverlayAdded = false")
        }
    }

    private fun dismissOverlay() {
        android.util.Log.e("HabitOverlay", "[STATE] dismissOverlay (total). Calling dismissOverlayViewOnly + queue callback")
        dismissOverlayViewOnly()
        OverlayQueueManager.getInstance(applicationContext).onOverlayDismissed()
        stopSelf()
    }

    override fun onDestroy() {
        stopAndReleaseMediaPlayer()
        dismissOverlay()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

private class ServiceLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val controller = SavedStateRegistryController.create(this)

    init {
        controller.performRestore(null)
    }

    fun start() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    fun resume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry
}
