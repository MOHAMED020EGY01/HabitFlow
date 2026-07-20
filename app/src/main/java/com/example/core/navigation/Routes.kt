package com.example.core.navigation

import androidx.navigation.NavController

object Routes {
    const val SPLASH       = "splash"
    const val ONBOARDING   = "onboarding"
    const val MAIN_PAGER   = "main_pager?initialTab={initialTab}"
    const val ADD_HABIT    = "add_habit?habitId={habitId}"
    const val HABIT_DETAIL = "habit_detail/{habitId}"
    const val CALENDAR     = "calendar"
    const val NOTIFICATIONS = "notifications"

    // Tab Constants
    const val HOME         = "home"
    const val ALL_HABITS   = "all_habits"
    const val SUMMARY      = "summary"
    const val SETTINGS     = "settings"
}

fun NavController.navigateToTopLevelDestination(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
