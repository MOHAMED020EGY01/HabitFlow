package com.example.app

import com.example.R
import com.example.BuildConfig
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.feature.main.presentation.MainPagerScreen
import com.example.core.navigation.Routes
import com.example.core.navigation.rememberNavigationMotionEngine
import com.example.core.navigation.MainTab
import com.example.core.navigation.NavAnimations
import com.example.feature.habit.presentation.AddHabitScreen
import com.example.feature.habit.presentation.HabitDetailScreen
import com.example.feature.onboarding.presentation.OnboardingScreen
import com.example.feature.splash.presentation.SplashScreen
import com.example.feature.calendar.presentation.CalendarScreen
import com.example.feature.notifications.presentation.NotificationsScreen
import com.example.core.ui.theme.HabitFlowTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.core.ui.BottomNavBar
import com.example.core.navigation.navigateToTopLevelDestination

class MainActivity : AppCompatActivity() {
    private val deepLinkRoute = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    private val memoryPressureCallback = object : ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) {
            when {
                level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                    android.util.Log.d("MainActivity", "TRIM_MEMORY_COMPLETE - heavy pressure")
                    System.gc()
                }
                level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                    android.util.Log.d("MainActivity", "TRIM_MEMORY_MODERATE")
                }
                level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                    android.util.Log.d("MainActivity", "TRIM_MEMORY_UI_HIDDEN")
                }
            }
        }

        override fun onConfigurationChanged(newConfig: Configuration) {}

        override fun onLowMemory() {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val app = application as HabitApplication

        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !app.isFullyInitialized() }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        application.registerComponentCallbacks(memoryPressureCallback)

        intent?.getStringExtra("DEEP_LINK_ROUTE")?.let {
            deepLinkRoute.value = it
        }

        setContent {
            // val appTheme by app.preferencesManager.appThemeFlow.collectAsState(initial = "system")
            // Force Dark Mode always — Task 4 fix
            val darkTheme = true
            /*
            val darkTheme = when (appTheme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            */

            val appLanguage by app.preferencesManager.appLanguageFlow.collectAsState(initial = app.currentLanguageCode)
            val isCardAnimationsEnabled by app.preferencesManager.isCardAnimationsEnabledFlow.collectAsState(initial = true)
            val glassEffectMode by app.preferencesManager.glassEffectModeFlow.collectAsState(initial = 0)
            val isNavBarHidden by app.preferencesManager.isNavBarHiddenFlow.collectAsState(initial = true)

            // Apply immersive nav bar (sticky swipe-to-reveal)
            // Re-applied on every onResume so it survives config changes/system dialogs
            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, isNavBarHidden) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        val controller = WindowInsetsControllerCompat(window, window.decorView)
                        if (isNavBarHidden) {
                            controller.hide(WindowInsetsCompat.Type.navigationBars())
                            controller.systemBarsBehavior =
                                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        } else {
                            controller.show(WindowInsetsCompat.Type.navigationBars())
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val navController = rememberNavController()

            val deepLink by deepLinkRoute.collectAsState()
            LaunchedEffect(deepLink) {
                val route = deepLink
                if (route != null) {
                    val targetRoute = if (route == "add_habit") "add_habit?habitId=0" else route
                    try {
                        delay(400)
                        navController.navigate(targetRoute)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    deepLinkRoute.value = null
                }
            }

            HabitFlowTheme(darkTheme = darkTheme) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val localizedContext = androidx.compose.runtime.remember(context, appLanguage) {
                    com.example.core.util.LocaleDirectionHelper.getLocalizedContext(context, appLanguage)
                }
                val direction = com.example.core.util.LocaleDirectionHelper.getLayoutDirection(appLanguage)
                val configuration = localizedContext.resources.configuration
                val activity = this@MainActivity
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalContext provides localizedContext,
                    androidx.compose.ui.platform.LocalConfiguration provides configuration,
                    LocalLayoutDirection provides direction,
                    androidx.activity.compose.LocalActivityResultRegistryOwner provides activity,
                    androidx.activity.compose.LocalOnBackPressedDispatcherOwner provides activity,
                    com.example.core.ui.LocalCardAnimationsEnabled provides isCardAnimationsEnabled,
                    com.example.core.ui.LocalGlassEffectMode provides glassEffectMode
                ) {
                    com.example.core.ui.CardAnimationClockProvider(
                        enabled = isCardAnimationsEnabled
                    ) {
                        val isRtl = com.example.core.util.LocaleDirectionHelper.isRtl(appLanguage)
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val rawRoute = navBackStackEntry?.destination?.route
                        val lastNonNullRoute = androidx.compose.runtime.saveable.rememberSaveable {
                            mutableStateOf<String?>(null)
                        }
                        LaunchedEffect(rawRoute) {
                            if (rawRoute != null) {
                                lastNonNullRoute.value = rawRoute
                            }
                        }
                        val currentRoute = rawRoute ?: lastNonNullRoute.value

                        AppNavigation(
                            navController = navController,
                            isRtl = isRtl,
                            currentRoute = currentRoute,
                            navBarPaddingRequired = !isNavBarHidden
                        )
                    }
                }
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        memoryPressureCallback.onTrimMemory(level)
    }

    override fun onResume() {
        super.onResume()
        val app = application as HabitApplication
        val appContext = app.applicationContext

        app.applicationScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val isEnabled = app.preferencesManager.isBackgroundServiceEnabledFlow.first()
                if (isEnabled) {
                    val habits = app.repository.getAllHabitsSync()

                    habits.forEach { habit ->
                        if (habit.isActive) {
                            com.example.core.infrastructure.worker.HabitReminderWorker.scheduleHabitReminders(
                                appContext, habit
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("DEEP_LINK_ROUTE")?.let {
            deepLinkRoute.value = it
        }
    }

    override fun onDestroy() {
        application.unregisterComponentCallbacks(memoryPressureCallback)
        super.onDestroy()
    }
}

@androidx.compose.runtime.Composable
private fun AppNavigation(
    navController: NavHostController,
    isRtl: Boolean,
    currentRoute: String?,
    navBarPaddingRequired: Boolean = true
) {
    val enterTransition = NavAnimations.enterTransition(isRtl)
    val exitTransition = NavAnimations.exitTransition(isRtl)
    val popEnterTransition = NavAnimations.popEnterTransition(isRtl)
    val popExitTransition = NavAnimations.popExitTransition(isRtl)

    val engine = rememberNavigationMotionEngine()
    val isMainPagerActive = currentRoute?.startsWith(Routes.MAIN_PAGER.split("?").first()) == true

    Scaffold(
        bottomBar = {
            com.example.core.ui.BottomNavBar(
                engine = engine,
                visible = isMainPagerActive,
                navBarPaddingRequired = navBarPaddingRequired
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.SPLASH,
                enterTransition = enterTransition,
                exitTransition = exitTransition,
                popEnterTransition = popEnterTransition,
                popExitTransition = popExitTransition
            ) {
                composable(route = Routes.SPLASH) {
                    SplashScreen(navController = navController)
                }
                composable(route = Routes.ONBOARDING) {
                    OnboardingScreen(navController = navController)
                }
                
                composable(
                    route = Routes.MAIN_PAGER,
                    arguments = listOf(
                        navArgument("initialTab") {
                            type = NavType.IntType
                            defaultValue = 0
                        }
                    )
                ) { backStackEntry ->
                    val initialTabIndex = backStackEntry.arguments?.getInt("initialTab") ?: 0
                    
                    LaunchedEffect(initialTabIndex) {
                        engine.scrollTo(MainTab.entries[initialTabIndex])
                    }
                    
                    MainPagerScreen(
                        navController = navController,
                        engine = engine
                    )
                }

                composable(
                    route = Routes.ADD_HABIT,
                    arguments = listOf(
                        navArgument("habitId") {
                            type = NavType.IntType
                            defaultValue = 0
                        }
                    ),
                    enterTransition = enterTransition,
                    exitTransition = exitTransition,
                    popEnterTransition = popEnterTransition,
                    popExitTransition = popExitTransition
                ) { backStackEntry ->
                    val habitId = backStackEntry.arguments?.getInt("habitId") ?: 0
                    AddHabitScreen(navController = navController, habitId = habitId)
                }
                composable(
                    route = Routes.HABIT_DETAIL,
                    arguments = listOf(
                        navArgument("habitId") {
                            type = NavType.IntType
                        }
                    )
                ) { backStackEntry ->
                    val habitId = backStackEntry.arguments?.getInt("habitId") ?: 0
                    HabitDetailScreen(navController = navController, habitId = habitId)
                }
                composable(route = Routes.CALENDAR) {
                    CalendarScreen(navController = navController)
                }
                composable(route = Routes.NOTIFICATIONS) {
                    NotificationsScreen(navController = navController)
                }
            }
        }
    }
}
