package com.example.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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

    private val bottomNavRoutesList = listOf(
        Routes.HOME,
        Routes.ALL_HABITS,
        Routes.SUMMARY,
        Routes.SETTINGS
    )

    private val bottomNavRoutesSet = bottomNavRoutesList.toSet()

    private fun getTabIndex(route: String?): Int {
        if (route == null) return -1
        // Match template route ignoring arguments
        val cleanRoute = route.split("?").first().split("/").first()
        return bottomNavRoutesList.indexOfFirst { it.split("?").first().split("/").first() == cleanRoute }
    }

    private fun isSplashOrOnboarding(route: String?): Boolean {
        if (route == null) return false
        val cleanRoute = route.split("?").first().split("/").first()
        return cleanRoute == Routes.SPLASH || cleanRoute == Routes.ONBOARDING
    }

    @Composable
    fun enterTransition(isRtl: Boolean): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition {
        val animationsEnabled = com.example.core.ui.LocalCardAnimationsEnabled.current
        return {
            if (!animationsEnabled) {
                EnterTransition.None
            } else {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route

                val initialTabIndex = getTabIndex(initialRoute)
                val targetTabIndex = getTabIndex(targetRoute)

                if (initialTabIndex != -1 && targetTabIndex != -1) {
                    // Tab switch: slide based on relative position
                    val isForward = targetTabIndex > initialTabIndex
                    val rtlFactor = if (isRtl) -1 else 1
                    val slideDirection = if (isForward) rtlFactor else -rtlFactor
                    slideInHorizontally(
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING),
                        initialOffsetX = { width -> (width * slideDirection).toInt() }
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING)
                    )
                } else if (isSplashOrOnboarding(initialRoute) || isSplashOrOnboarding(targetRoute)) {
                    // Splash / Onboarding transitions: premium fade + scale
                    fadeIn(
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING)
                    ) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING)
                    )
                } else {
                    // General forward navigation (e.g. Home -> Add / Detail)
                    val rtlFactor = if (isRtl) -1 else 1
                    slideInHorizontally(
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING),
                        initialOffsetX = { width -> (width * rtlFactor).toInt() }
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING)
                    )
                }
            }
        }
    }

    @Composable
    fun exitTransition(isRtl: Boolean): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition {
        val animationsEnabled = com.example.core.ui.LocalCardAnimationsEnabled.current
        return {
            if (!animationsEnabled) {
                ExitTransition.None
            } else {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route

                val initialTabIndex = getTabIndex(initialRoute)
                val targetTabIndex = getTabIndex(targetRoute)

                if (initialTabIndex != -1 && targetTabIndex != -1) {
                    // Tab switch
                    val isForward = targetTabIndex > initialTabIndex
                    val rtlFactor = if (isRtl) -1 else 1
                    val slideDirection = if (isForward) -rtlFactor else rtlFactor
                    slideOutHorizontally(
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING),
                        targetOffsetX = { width -> (width * slideDirection).toInt() }
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING)
                    )
                } else if (isSplashOrOnboarding(initialRoute) || isSplashOrOnboarding(targetRoute)) {
                    // Splash / Onboarding: fade + scale out
                    fadeOut(
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING)
                    ) + scaleOut(
                        targetScale = 0.95f,
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING)
                    )
                } else {
                    // General forward exit (outgoing screen slides out slightly left and fades)
                    val rtlFactor = if (isRtl) -1 else 1
                    slideOutHorizontally(
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING),
                        targetOffsetX = { width -> -(width * 0.3f * rtlFactor).toInt() }
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING)
                    )
                }
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
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route

                val initialTabIndex = getTabIndex(initialRoute)
                val targetTabIndex = getTabIndex(targetRoute)

                if (initialTabIndex != -1 && targetTabIndex != -1) {
                    // Tab switch (backwards)
                    val isForward = targetTabIndex > initialTabIndex
                    val rtlFactor = if (isRtl) -1 else 1
                    val slideDirection = if (isForward) rtlFactor else -rtlFactor
                    slideInHorizontally(
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING),
                        initialOffsetX = { width -> (width * slideDirection).toInt() }
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING)
                    )
                } else if (isSplashOrOnboarding(initialRoute) || isSplashOrOnboarding(targetRoute)) {
                    fadeIn(
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING)
                    ) + scaleIn(
                        initialScale = 0.95f,
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING)
                    )
                } else {
                    // General back navigation (incoming screen slides in from left and fades in)
                    val rtlFactor = if (isRtl) -1 else 1
                    slideInHorizontally(
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING),
                        initialOffsetX = { width -> -(width * rtlFactor).toInt() }
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING)
                    )
                }
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
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route

                val initialTabIndex = getTabIndex(initialRoute)
                val targetTabIndex = getTabIndex(targetRoute)

                if (initialTabIndex != -1 && targetTabIndex != -1) {
                    // Tab switch (backwards)
                    val isForward = targetTabIndex > initialTabIndex
                    val rtlFactor = if (isRtl) -1 else 1
                    val slideDirection = if (isForward) -rtlFactor else rtlFactor
                    slideOutHorizontally(
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING),
                        targetOffsetX = { width -> (width * slideDirection).toInt() }
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING)
                    )
                } else if (isSplashOrOnboarding(initialRoute) || isSplashOrOnboarding(targetRoute)) {
                    fadeOut(
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING)
                    ) + scaleOut(
                        targetScale = 0.95f,
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING)
                    )
                } else {
                    // General back exit (outgoing screen slides out completely right and fades)
                    val rtlFactor = if (isRtl) -1 else 1
                    slideOutHorizontally(
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING),
                        targetOffsetX = { width -> (width * rtlFactor).toInt() }
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = TRANSITION_DURATION, easing = TRANSITION_EASING)
                    )
                }
            }
        }
    }
}
