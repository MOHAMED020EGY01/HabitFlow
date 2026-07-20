package com.example.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import androidx.compose.runtime.Composable

object NavAnimations {

    private const val TRANSITION_DURATION = 350
    private val TRANSITION_EASING = FastOutSlowInEasing

    private fun isSplashOrOnboarding(route: String?): Boolean {
        if (route == null) return false
        val cleanRoute = route.split("?").first().split("/").first()
        return cleanRoute == Routes.SPLASH || cleanRoute == Routes.ONBOARDING
    }

    private fun isMainPager(route: String?): Boolean {
        if (route == null) return false
        return route.startsWith(Routes.MAIN_PAGER.split("?").first())
    }

    @Composable
    fun enterTransition(isRtl: Boolean): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition {
        val animationsEnabled = com.example.core.ui.LocalCardAnimationsEnabled.current
        return {
            val initialRoute = initialState.destination.route
            val targetRoute = targetState.destination.route

            if (isMainPager(initialRoute) && isMainPager(targetRoute)) {
                EnterTransition.None
            } else if (!animationsEnabled) {
                EnterTransition.None
            } else if (isSplashOrOnboarding(initialRoute) || isSplashOrOnboarding(targetRoute)) {
                fadeIn(tween(TRANSITION_DURATION, easing = TRANSITION_EASING)) + 
                scaleIn(initialScale = 0.95f, animationSpec = tween(TRANSITION_DURATION, easing = TRANSITION_EASING))
            } else {
                val rtlFactor = if (isRtl) -1 else 1
                slideInHorizontally(
                    animationSpec = tween(TRANSITION_DURATION, easing = TRANSITION_EASING),
                    initialOffsetX = { width -> (width * rtlFactor).toInt() }
                ) + fadeIn(tween(TRANSITION_DURATION, easing = TRANSITION_EASING))
            }
        }
    }

    @Composable
    fun exitTransition(isRtl: Boolean): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition {
        val animationsEnabled = com.example.core.ui.LocalCardAnimationsEnabled.current
        return {
            val initialRoute = initialState.destination.route
            val targetRoute = targetState.destination.route

            if (isMainPager(initialRoute) && isMainPager(targetRoute)) {
                ExitTransition.None
            } else if (!animationsEnabled) {
                ExitTransition.None
            } else if (isSplashOrOnboarding(initialRoute) || isSplashOrOnboarding(targetRoute)) {
                fadeOut(tween(TRANSITION_DURATION, easing = TRANSITION_EASING)) + 
                scaleOut(targetScale = 0.95f, animationSpec = tween(TRANSITION_DURATION, easing = TRANSITION_EASING))
            } else {
                val rtlFactor = if (isRtl) -1 else 1
                slideOutHorizontally(
                    animationSpec = tween(TRANSITION_DURATION, easing = TRANSITION_EASING),
                    targetOffsetX = { width -> -(width * 0.3f * rtlFactor).toInt() }
                ) + fadeOut(tween(TRANSITION_DURATION, easing = TRANSITION_EASING))
            }
        }
    }

    @Composable
    fun popEnterTransition(isRtl: Boolean): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition {
        val animationsEnabled = com.example.core.ui.LocalCardAnimationsEnabled.current
        return {
            if (!animationsEnabled) {
                EnterTransition.None
            } else {
                val rtlFactor = if (isRtl) -1 else 1
                slideInHorizontally(
                    animationSpec = tween(TRANSITION_DURATION, easing = TRANSITION_EASING),
                    initialOffsetX = { width -> -(width * rtlFactor).toInt() }
                ) + fadeIn(tween(TRANSITION_DURATION, easing = TRANSITION_EASING))
            }
        }
    }

    @Composable
    fun popExitTransition(isRtl: Boolean): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition {
        val animationsEnabled = com.example.core.ui.LocalCardAnimationsEnabled.current
        return {
            if (!animationsEnabled) {
                ExitTransition.None
            } else {
                val rtlFactor = if (isRtl) -1 else 1
                slideOutHorizontally(
                    animationSpec = tween(TRANSITION_DURATION, easing = TRANSITION_EASING),
                    targetOffsetX = { width -> (width * rtlFactor).toInt() }
                ) + fadeOut(tween(TRANSITION_DURATION, easing = TRANSITION_EASING))
            }
        }
    }
}
