package com.example.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.util.DeviceCapability
import kotlin.math.hypot

// ─────────────────────────────────────────────────────────────────────────────
// CompositionLocals
// ─────────────────────────────────────────────────────────────────────────────

/**
 * CompositionLocal set to true when card animations ("reduced effects") are enabled.
 */
val LocalCardAnimationsEnabled = compositionLocalOf { true }

/**
 * CompositionLocal that controls the "glass effect" (background blur).
 * Values:
 * - `0`: Auto (use [DeviceCapability.isBlurCapableDevice] heuristic)
 * - `1`: Force On
 * - `2`: Force Off
 */
val LocalGlassEffectMode = compositionLocalOf { 0 }

// ─────────────────────────────────────────────────────────────────────────────
// GlassCard — Animated Glassmorphism (Part 95)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A frosted-glass card with a diagonal glass reflection sweep (mirror /
 * Apple / VisionOS-style) and an optional animated border accent.
 *
 * ## Reflection sweep
 * A narrow, translucent white linear gradient that travels diagonally
 * from top-left to bottom-right across the card surface, then wraps
 * around.  The motion is one-way (no back-and-forth), with a constant
 * linear speed, giving a realistic glossy glass sheen effect.
 *
 * ## Accent border (optional)
 * A thin static border in the card's accent colour ([habitColor] or
 * theme primary).  When [glowColor] is non-null, a rotating SweepGradient
 * comet border replaces the static border (see [glowColor] parameter).
 *
 * ## Performance
 * Both the reflection and (optionally) the comet border read the shared
 * clock ([LocalCardAnimationClock]) **only inside `onDrawWithContent`**
 * — never via `by` in the composable function body (Parts 58-60 / 88).
 *
 * @param modifier          Outer modifier.
 * @param habitColor        Accent colour; falls back to theme primary.
 * @param onClick           Optional click handler.
 * @param shape             Border shape; defaults to 24.dp rounded rect.
 * @param borderWidth       Stroke width for the comet / static border.
 * @param animationDelayMs  Per-instance phase offset in ms.
 * @param glowColor         When non-null, replaces the static border with a
 *                          rotating SweepGradient comet.  Defaults to
 *                          [habitColor] for habit-specific cards.
 * @param glowCornerRadius  Corner radius for the comet gradient.
 * @param content           Card children.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    habitColor: Color? = null,
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(24.dp),
    borderWidth: Dp = 1.5.dp,
    animationDelayMs: Int = 0,
    glowColor: Color? = habitColor,
    glowCornerRadius: Dp = 24.dp,
    fillAlpha: Float? = null,
    isScrolling: Boolean = false, // New parameter to optimize performance
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val defaultPrimary = MaterialTheme.colorScheme.primary
    val accentColor = habitColor ?: defaultPrimary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val context = LocalContext.current

    // ── Glass effect mode (blur on/off) ─────────────────────────────────
    val glassEffectMode = LocalGlassEffectMode.current
    val deviceBlurCapable = remember(context) { DeviceCapability.isBlurCapableDevice() }
    val shouldBlur = remember(glassEffectMode, deviceBlurCapable, isScrolling) {
        if (isScrolling) false else { // Disable blur during scroll for high performance
            when (glassEffectMode) {
                0 -> deviceBlurCapable       // Auto
                1 -> true                    // Force On
                else -> false                // Force Off
            }
        }
    }

    // ── Translucent glass fill ───────────────────────────────────────────
    val glassFillAlpha = fillAlpha ?: (if (isDark) 0.92f else 0.95f)
    val glassFillColor = remember(surfaceColor, glassFillAlpha) {
        surfaceColor.copy(alpha = glassFillAlpha)
    }

    // ── Animations on/off ────────────────────────────────────────────────
    val isAnimationsEnabled = LocalCardAnimationsEnabled.current && !isScrolling
    val cometColor = glowColor ?: accentColor

    // Read the shared clock State reference ONCE in the composable body.
    val clockState = LocalCardAnimationClock.current

    // Cached values accessed only at draw time.
    val cachedAccentColor = accentColor
    val cachedCometColor = cometColor
    val cachedAnimationDelayMs = animationDelayMs
    val cachedBorderWidth = borderWidth
    val cachedCometCornerRadius = glowCornerRadius

    // ── Static border colour (animations off) ──────────────────────────
    val borderColorStatic = accentColor.copy(alpha = 0.35f)

    // ── Cached SweepGradient holder (comet, rebuilt on colour/size) ─────
    val sweepShaderHolder = remember(cachedCometColor, isAnimationsEnabled) {
        object {
            var shader: android.graphics.Shader? = null
            var lastSize: androidx.compose.ui.geometry.Size? = null
            val matrix = android.graphics.Matrix()
        }
    }

    // ── Draw-time modifier (reflection sweep + optional comet) ──────────
    val drawModifier = remember(
        cachedAccentColor, cachedCometColor, cachedBorderWidth,
        shape, isAnimationsEnabled, cachedAnimationDelayMs,
        cachedCometCornerRadius
    ) {
        Modifier.drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val borderStroke = Stroke(width = cachedBorderWidth.toPx())
            val outlineForBorder = outline  // capture for inner lambda

            onDrawWithContent {
                drawContent()

                val sharedClockMs = clockState.floatValue
                val cycleMs = CARD_ANIMATION_CYCLE_MS.toFloat()
                val effectiveMs = (sharedClockMs + cachedAnimationDelayMs) % cycleMs
                val progress = effectiveMs / cycleMs  // monotonic 0→1, one-way

                // ── Animated border (optional comet) ────────────────────
                if (isAnimationsEnabled && cachedCometColor != null) {
                    val angle = progress * 360f

                    if (sweepShaderHolder.shader == null ||
                        sweepShaderHolder.lastSize != size
                    ) {
                        val base = cachedCometColor
                        val argbBase = base.toArgb()
                        val argbLight = Color(
                            red = (base.red + 1f) / 2f,
                            green = (base.green + 1f) / 2f,
                            blue = (base.blue + 1f) / 2f,
                            alpha = 1f
                        ).toArgb()
                        val argbDark = Color(
                            red = (base.red * 0.3f).coerceIn(0f, 1f),
                            green = (base.green * 0.3f).coerceIn(0f, 1f),
                            blue = (base.blue * 0.3f).coerceIn(0f, 1f),
                            alpha = 1f
                        ).toArgb()
                        sweepShaderHolder.shader = android.graphics.SweepGradient(
                            size.width / 2f, size.height / 2f,
                            intArrayOf(argbBase, argbLight, argbDark, argbLight, argbBase),
                            null
                        )
                        sweepShaderHolder.lastSize = size
                    }

                    sweepShaderHolder.matrix.reset()
                    sweepShaderHolder.matrix.postRotate(
                        angle, size.width / 2f, size.height / 2f
                    )
                    sweepShaderHolder.shader!!.setLocalMatrix(sweepShaderHolder.matrix)

                    drawOutline(
                        outline = outline,
                        brush = object : androidx.compose.ui.graphics.ShaderBrush() {
                            override fun createShader(
                                shaderSize: androidx.compose.ui.geometry.Size
                            ): android.graphics.Shader {
                                return sweepShaderHolder.shader!!
                            }
                        },
                        style = borderStroke
                    )
                } else if (isAnimationsEnabled) {
                    // ── Static accent border (no comet, animations on) ──
                    drawOutline(
                        outline = outline,
                        color = borderColorStatic,
                        style = borderStroke
                    )
                } else {
                    // ── Static border (animations off) ──────────────────
                    drawOutline(
                        outline = outline,
                        color = borderColorStatic,
                        style = borderStroke
                    )
                }

                // ── Subtle diagonal glass reflection (surface sweep) ────
                if (isAnimationsEnabled) {
                    val diagonal = hypot(
                        size.width.toDouble(), size.height.toDouble()
                    ).toFloat()
                    val bandLen = diagonal * 0.55f
                    val overshoot = diagonal * 0.35f

                    // Band centre moves from off-screen top-left to bottom-right
                    val centerX = androidx.compose.ui.util.lerp(-overshoot, size.width + overshoot, progress)
                    val centerY = androidx.compose.ui.util.lerp(-overshoot, size.height + overshoot, progress)

                    val reflectionGradient = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0f),
                            Color.White.copy(alpha = 0.04f),
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.04f),
                            Color.White.copy(alpha = 0f)
                        ),
                        start = Offset(centerX - bandLen * 0.5f, centerY - bandLen * 0.5f),
                        end = Offset(centerX + bandLen * 0.5f, centerY + bandLen * 0.5f)
                    )

                    // Fill the card surface with the gradient — clipped by outline
                    drawOutline(
                        outline = outline,
                        brush = reflectionGradient,
                        style = Fill
                    )
                }
            }
        }
    }

    // ── Outer layout ──────────────────────────────────────────────────────
    Box(modifier = modifier) {
        // Background layer — translucent fill + conditional blur
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(glassFillColor)
                .then(
                    if (shouldBlur) Modifier.blur(8.dp) else Modifier
                )
        )
        // Content layer — with reflection sweep
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .then(drawModifier)
        ) {
            content()
        }
    }
}
