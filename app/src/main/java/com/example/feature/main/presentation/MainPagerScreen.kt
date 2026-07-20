package com.example.feature.main.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.core.navigation.LocalNavigationMotionEngine
import com.example.core.navigation.NavigationMotionEngine
import com.example.feature.home.presentation.HomeScreen
import com.example.feature.habit.presentation.AllHabitsScreen
import com.example.feature.summary.presentation.SummaryScreen
import com.example.feature.settings.presentation.SettingsScreen

@Composable
fun MainPagerScreen(
    navController: NavController,
    engine: NavigationMotionEngine
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    CompositionLocalProvider(LocalNavigationMotionEngine provides engine) {
        HorizontalPager(
            state = engine.pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            userScrollEnabled = true,
            key = { it }
        ) { page ->
            saveableStateHolder.SaveableStateProvider(key = page) {
                when (page) {
                    0 -> HomeScreen(navController = navController)
                    1 -> AllHabitsScreen(navController = navController)
                    2 -> SummaryScreen(navController = navController)
                    3 -> SettingsScreen(navController = navController)
                }
            }
        }
    }
}
