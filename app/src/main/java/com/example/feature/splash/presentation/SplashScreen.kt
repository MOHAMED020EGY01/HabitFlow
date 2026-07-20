package com.example.feature.splash.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.core.navigation.Routes
import com.example.app.HabitApplication
import kotlinx.coroutines.flow.first

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        val app = HabitApplication.instance
        val isOnboardingComplete = app.preferencesManager.isOnboardingCompleteFlow.first()
        
        if (isOnboardingComplete) {
            navController.navigate(Routes.MAIN_PAGER.replace("{initialTab}", "0")) {
                popUpTo(Routes.SPLASH) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            navController.navigate(Routes.ONBOARDING) {
                popUpTo(Routes.SPLASH) { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
