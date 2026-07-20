package com.example.core.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Duration of one full shimmer / border-glow cycle in milliseconds.
 * Matches the original 6000 ms from Part 68 (reduced from 9000 ms
 * after Part 77's recommendation to stay smooth).
 */
const val CARD_ANIMATION_CYCLE_MS = 6_000

/**
 * Stagger offset applied per card index so that adjacent cards
 * do not animate in lockstep.  Cards with [GlassCard.animationDelayMs]
 * get an additional per-instance phase shift on top of this.
 */
const val CARD_ANIMATION_STAGGER_MS_PER_INDEX = 150

/**
 * Shared single-infinite-transition time value as a [State<Float>].
 *
 * Every [GlassCard] that participates in the shimmer/border animation
 * reads this value's `.value` (or `.floatValue`) **only inside
 * [androidx.compose.ui.draw.onDrawWithContent]** — never via `by` in
 * the composable body — to avoid triggering full recomposition at 60 fps.
 *
 * The actual per-card effective progress is computed as:
 * ```
 * effectiveMs = (clockState.value + animationDelayMs) % CYCLE_MS
 * effectiveProgress = effectiveMs / CYCLE_MS
 * ```
 */
val LocalCardAnimationClock = staticCompositionLocalOf { mutableFloatStateOf(0f) }

/**
 * Wraps [content] with a single shared infinite-transition clock.
 *
 * Place this once at the top of your composition tree (e.g. in
 * [com.example.MainActivity]) so every card on every screen reads
 * the *same* animated value rather than creating per-card clocks.
 *
 * When [enabled] is `false` the clock stays at 0f, producing no motion.
 */
@Composable
fun CardAnimationClockProvider(
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    val clockState = remember { mutableFloatStateOf(0f) }

    if (enabled) {
        // Raw monotonic sawtooth: 0 → CYCLE_MS → Restart (jumps back to 0).
        // Only created when enabled — avoids running an infinite animation loop
        // that kept the composition pipeline busy even when no cards were visible.
        val transition = rememberInfiniteTransition(label = "SharedCardAnimationClock")
        val clockValue by transition.animateFloat(
            initialValue = 0f,
            targetValue = CARD_ANIMATION_CYCLE_MS.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = CARD_ANIMATION_CYCLE_MS,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "SharedCardAnimationClockValue"
        )

        clockState.floatValue = clockValue
    } else {
        clockState.floatValue = 0f
    }

    CompositionLocalProvider(LocalCardAnimationClock provides clockState) {
        content()
    }
}

/** Shared easing applied to the per-card triangle progress (Part 88). */
val CardAnimationEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
