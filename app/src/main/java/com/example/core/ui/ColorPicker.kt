package com.example.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 7 preset colors chosen for maximum visual spread:
 * Red → Orange → Yellow → Green → Blue → Purple → Gray.
 *
 * Removed from original 12:
 * - Pink  (#D81B60) — too close to Red
 * - Teal  (#00897B) — too close to Green and Blue
 * - Cyan  (#00ACC1) — too close to Blue and Teal
 * - Indigo (#3949AB) — too close to Blue and Purple
 * - Brown (#6D4C41) — too close to Orange
 */
val PresetColors = listOf(
    "#E53935", // Red
    "#FB8C00", // Orange
    "#FDD835", // Yellow
    "#43A047", // Green
    "#1E88E5", // Blue
    "#8E24AA", // Purple
    "#757575"  // Gray
)

// Rainbow / multi-colour gradient for the Custom swatch
private val customSwatchBrush = Brush.sweepGradient(
    colors = listOf(
        Color.Red,
        Color.Yellow,
        Color.Green,
        Color.Cyan,
        Color.Blue,
        Color.Magenta,
        Color.Red
    )
)

@Composable
fun ColorPicker(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine if the currently selected colour matches a preset or is custom
    val customMatch = remember(selectedColorHex) {
        selectedColorHex !in PresetColors
    }
    // Track whether the Custom swatch is selected (either by matching a
    // saved custom colour, or after the user picks a custom colour)
    var showCustomPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Title (Outside Card) ──────────────────────────────────
        Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.choose_color),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            habitColor = try {
                Color(android.graphics.Color.parseColor(selectedColorHex))
            } catch (_: Exception) {
                MaterialTheme.colorScheme.primary
            }
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── 2 rows × 4 swatches ───────────────────────────────
                // Row 1: presets 0..3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PresetColors.take(4).forEach { colorHex ->
                        ColorSwatch(
                            colorHex = colorHex,
                            isSelected = selectedColorHex.equals(colorHex, ignoreCase = true),
                            onClick = { onColorSelected(colorHex) }
                        )
                    }
                }

                // Row 2: presets 4..6 + Custom swatch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PresetColors.drop(4).forEach { colorHex ->
                        ColorSwatch(
                            colorHex = colorHex,
                            isSelected = selectedColorHex.equals(colorHex, ignoreCase = true),
                            onClick = { onColorSelected(colorHex) }
                        )
                    }

                    // ── Custom swatch ──────────────────────────────────
                    val isCustomSelected = customMatch
                    val customScale by animateFloatAsState(
                        targetValue = if (isCustomSelected) 1.15f else 1.0f,
                        label = "customScale"
                    )
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .scale(customScale)
                            .clip(CircleShape)
                            .background(customSwatchBrush)
                            .clickable { showCustomPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCustomSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.color_custom),
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Custom color picker dialog ────────────────────────────────
    if (showCustomPicker) {
        CustomColorPickerDialog(
            initialHex = if (customMatch) selectedColorHex else PresetColors.first(),
            onSelect = { chosenHex ->
                showCustomPicker = false
                onColorSelected(chosenHex)
            },
            onDismiss = { showCustomPicker = false }
        )
    }
}

/**
 * A single preset colour circle with selection highlight and animation.
 */
@Composable
private fun ColorSwatch(
    colorHex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        label = "scale_$colorHex"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
