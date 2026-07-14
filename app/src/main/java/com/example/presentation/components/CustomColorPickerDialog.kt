package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * A full custom color picker dialog opened when the user taps the "Custom"
 * swatch in [ColorPicker].
 *
 * Contains:
 * - A live colour preview circle
 * - Hue, Saturation, and Value (Brightness) sliders
 * - A hex-code text input field
 * - Select (OK) / Cancel actions
 *
 * @param initialHex  The hex color to start with (e.g. the habit's saved color
 *                    or the last-selected preset).
 * @param onSelect    Called with the chosen hex string when the user taps Select.
 * @param onDismiss   Called when the dialog is dismissed (Cancel or back press).
 */
@Composable
fun CustomColorPickerDialog(
    initialHex: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Parse initial hex into HSV components
    val initialHsv = remember(initialHex) {
        try {
            val c = android.graphics.Color.parseColor(initialHex)
            val hsv = FloatArray(3)
            android.graphics.Color.RGBToHSV(
                (c shr 16) and 0xFF,
                (c shr 8) and 0xFF,
                c and 0xFF,
                hsv
            )
            hsv
        } catch (_: Exception) {
            floatArrayOf(0f, 0f, 1f)
        }
    }

    // Mutable state for the adjusted colour
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    // Derived current Compose colour and hex string
    val currentColor = remember(hue, saturation, value) {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        Color(argb)
    }

    val currentHex = remember(hue, saturation, value) {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        String.format("#%06X", argb and 0xFFFFFF)
    }

    // Hex text input — allow manual editing
    var hexText by remember(currentHex) { mutableStateOf(currentHex) }

    // When sliders change, update the hex text too (but don't override while
    // the user is actively typing — they'll press Select to confirm)
    val textFieldSynced = remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Title ─────────────────────────────────────────────
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.R.string.color_custom_picker_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // ── Live preview circle ───────────────────────────────
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .background(currentColor)
                )

                // ── Hue slider ────────────────────────────────────────
                Text(
                    text = "Hue",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = hue,
                    onValueChange = { newHue ->
                        hue = newHue
                        if (textFieldSynced.value) {
                            hexText = String.format(
                                "#%06X",
                                android.graphics.Color.HSVToColor(floatArrayOf(newHue, saturation, value)) and 0xFFFFFF
                            )
                        }
                    },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = currentColor,
                        activeTrackColor = currentColor.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Saturation slider ─────────────────────────────────
                Text(
                    text = "Saturation",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = saturation,
                    onValueChange = { newSat ->
                        saturation = newSat
                        if (textFieldSynced.value) {
                            hexText = String.format(
                                "#%06X",
                                android.graphics.Color.HSVToColor(floatArrayOf(hue, newSat, value)) and 0xFFFFFF
                            )
                        }
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = currentColor,
                        activeTrackColor = currentColor.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Value / Brightness slider ─────────────────────────
                Text(
                    text = "Brightness",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = value,
                    onValueChange = { newVal ->
                        value = newVal
                        if (textFieldSynced.value) {
                            hexText = String.format(
                                "#%06X",
                                android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, newVal)) and 0xFFFFFF
                            )
                        }
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = currentColor,
                        activeTrackColor = currentColor.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Hex input ─────────────────────────────────────────
                Text(
                    text = "Hex Code",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { newText ->
                        hexText = newText
                        textFieldSynced.value = false
                        // Try parsing; if valid, update the sliders
                        try {
                            val parsed = android.graphics.Color.parseColor(newText)
                            if (newText.length in 4..7) {
                                val hsv = FloatArray(3)
                                android.graphics.Color.RGBToHSV(
                                    (parsed shr 16) and 0xFF,
                                    (parsed shr 8) and 0xFF,
                                    parsed and 0xFF,
                                    hsv
                                )
                                hue = hsv[0]
                                saturation = hsv[1]
                                value = hsv[2]
                            }
                        } catch (_: Exception) { /* invalid hex — ignore */ }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Action buttons ────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.example.R.string.cancel),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            // Resolve final hex from the text field or sliders
                            val finalHex = try {
                                val parsed = android.graphics.Color.parseColor(hexText)
                                String.format("#%06X", parsed and 0xFFFFFF)
                            } catch (_: Exception) {
                                currentHex
                            }
                            onSelect(finalHex)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.example.R.string.color_custom_select),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
