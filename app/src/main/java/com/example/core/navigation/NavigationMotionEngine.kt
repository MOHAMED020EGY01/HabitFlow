package com.example.core.navigation

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
class NavigationMotionEngine(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope
) {
    val currentTab: MainTab
        get() = MainTab.entries[pagerState.currentPage]

    val targetTab: MainTab
        get() = MainTab.entries[pagerState.targetPage]

    val progress: Float
        get() = pagerState.currentPage + pagerState.currentPageOffsetFraction

    val isAnimating: Boolean
        get() = pagerState.isScrollInProgress

    fun navigateTo(tab: MainTab) {
        if (pagerState.targetPage != tab.ordinal) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(
                    page = tab.ordinal,
                    animationSpec = NavigationAnimationSpec
                )
            }
        }
    }

    fun scrollTo(tab: MainTab) {
        coroutineScope.launch {
            pagerState.scrollToPage(tab.ordinal)
        }
    }

    companion object {
        val NavigationAnimationSpec: AnimationSpec<Float> = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        )
    }
}

val LocalNavigationMotionEngine = staticCompositionLocalOf<NavigationMotionEngine?> { null }

@Composable
fun rememberNavigationMotionEngine(
    initialTabIndex: Int = 0
): NavigationMotionEngine {
    val pagerState = rememberPagerState(
        initialPage = initialTabIndex
    ) { MainTab.entries.size }
    val coroutineScope = rememberCoroutineScope()
    return remember(pagerState, coroutineScope) {
        NavigationMotionEngine(pagerState, coroutineScope)
    }
}
