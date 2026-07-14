package com.example.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * An [OutlinedTextField] with a static glass-style border and subtle
 * shimmer sweep (Part 86).  The animated comet border from Part 90/92
 * has been removed — this field is static in Part 93.
 *
 * When [isError] is `true` a static red error border is shown (error
 * always takes visual priority per Part 65).
 *
 * @param value             Current text value.
 * @param onValueChange     Callback when text changes.
 * @param habitColor        Accent colour for the border.
 * @param isError           When true, draws a static red error border.
 * @param singleLine        Whether the field is single-line.
 * @param placeholder       Placeholder composable.
 * @param supportingText    Supporting text / error message.
 * @param keyboardOptions   Keyboard options.
 * @param shape             Border shape; defaults to 12.dp rounded rect.
 * @param testTag           Test tag.
 * @param modifier          Outer modifier.
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    habitColor: Color? = null,
    isError: Boolean = false,
    singleLine: Boolean = false,
    placeholder: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    shape: Shape = RoundedCornerShape(12.dp),
    testTag: String? = null
) {
    val defaultPrimary = MaterialTheme.colorScheme.primary
    val actualHabitColor = habitColor ?: defaultPrimary
    val isAnimationsEnabled = LocalCardAnimationsEnabled.current
    val clockState = LocalCardAnimationClock.current

    var isFocused by remember { mutableStateOf(false) }

    val cachedColor = actualHabitColor
    val borderColor = cachedColor.copy(alpha = 0.35f)
    val errorColor = MaterialTheme.colorScheme.error

    // ── Shimmer-only draw modifier (Part 86) ──────────────────────────
    // Border is static Modifier.border (Part 93).
    val shimmerModifier = remember(cachedColor, shape, isAnimationsEnabled) {
        if (!isAnimationsEnabled) {
            Modifier.drawWithCache { onDrawWithContent { drawContent() } }
        } else {
            Modifier.drawWithCache {
                val outline = shape.createOutline(size, layoutDirection, this)
                onDrawWithContent {
                    drawContent()

                    val sharedClockMs = clockState.floatValue
                    val cycleMs = CARD_ANIMATION_CYCLE_MS.toFloat()
                    val effectiveMs = (sharedClockMs + 0) % cycleMs
                    val progress = effectiveMs / cycleMs

                    val focusFactor = if (isFocused) 0.5f else 1.0f

                    val shimmerGradient = Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0f),
                            Color.White.copy(alpha = 0.12f * focusFactor),
                            Color.White.copy(alpha = 0f)
                        ),
                        startX = size.width * (progress - 0.5f),
                        endX = size.width * (progress + 0.5f)
                    )
                    drawOutline(
                        outline = outline,
                        brush = shimmerGradient,
                        style = Stroke(width = 1.5.dp.toPx() * 3f)
                    )
                }
            }
        }
    }

    // ── Static border + shimmer overlay ─────────────────────────────────
    Box(
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .clip(shape)
            .then(
                when {
                    isError -> Modifier.border(
                        width = 1.5.dp,
                        color = errorColor,
                        shape = shape
                    )
                    else -> Modifier.border(
                        width = 1.dp,
                        color = borderColor,
                        shape = shape
                    )
                }
            )
            .then(shimmerModifier)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            placeholder = placeholder,
            isError = false,  // We handle the error border ourselves
            supportingText = supportingText,
            keyboardOptions = keyboardOptions,
            shape = shape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                errorBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(testTag?.let { Modifier.testTag(it) } ?: Modifier)
        )
    }
}
