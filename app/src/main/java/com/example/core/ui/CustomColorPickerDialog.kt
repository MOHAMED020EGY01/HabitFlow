package com.example.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomColorPickerDialog(
    initialHex: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
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

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }

    val currentColor = remember(hue, saturation, value) {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        Color(argb)
    }

    val currentHex = remember(hue, saturation, value) {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        String.format("%06X", argb and 0xFFFFFF)
    }

    var hexText by remember(currentHex) { mutableStateOf(currentHex) }

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
                Text(
                    text = stringResource(com.example.R.string.color_custom_picker_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Saturation-Value Square
                SaturationValuePicker(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onSaturationValueChange = { s, v ->
                        saturation = s
                        value = v
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                // Hue Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                    )

                    HuePicker(
                        hue = hue,
                        onHueChange = { hue = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Hex Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Hex",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { newText ->
                            val filtered = newText.filter { it.isDigit() || it.uppercaseChar() in 'A'..'F' }.take(6)
                            hexText = filtered
                            if (filtered.length == 6) {
                                try {
                                    val parsed = android.graphics.Color.parseColor("#$filtered")
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
                                } catch (_: Exception) {}
                            }
                        },
                        prefix = { Text("#") },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Presets Grid
                Text(
                    text = "Presets",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 8
                ) {
                    PresetColors.forEach { colorHex ->
                        val color = remember(colorHex) { Color(android.graphics.Color.parseColor(colorHex)) }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(color)
                                .clickable {
                                    try {
                                        val hsv = FloatArray(3)
                                        android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(colorHex), hsv)
                                        hue = hsv[0]
                                        saturation = hsv[1]
                                        value = hsv[2]
                                    } catch (_: Exception) {}
                                }
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(com.example.R.string.cancel),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSelect("#$hexText")
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(com.example.R.string.color_custom_select),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SaturationValuePicker(
    hue: Float,
    saturation: Float,
    value: Float,
    onSaturationValueChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(hue) {
                detectDragGestures { change, _ ->
                    val x = (change.position.x / size.width).coerceIn(0f, 1f)
                    val y = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onSaturationValueChange(x, y)
                }
            }
            .pointerInput(hue) {
                detectTapGestures { offset ->
                    val x = (offset.x / size.width).coerceIn(0f, 1f)
                    val y = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    onSaturationValueChange(x, y)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val hsvColor = android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
            val color = Color(hsvColor)

            drawRect(
                brush = Brush.horizontalGradient(listOf(Color.White, color))
            )
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black))
            )

            val thumbX = saturation * size.width
            val thumbY = (1f - value) * size.height
            
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = Offset(thumbX, thumbY),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = 10.dp.toPx(),
                center = Offset(thumbX, thumbY),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@Composable
fun HuePicker(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val x = (change.position.x / size.width).coerceIn(0f, 1f)
                    onHueChange(x * 360f)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val x = (offset.x / size.width).coerceIn(0f, 1f)
                    onHueChange(x * 360f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val colors = listOf(
                Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
            )
            drawRect(brush = Brush.horizontalGradient(colors))

            val thumbX = (hue / 360f) * size.width
            
            drawRect(
                color = Color.White,
                topLeft = Offset(thumbX - 2.dp.toPx(), -2.dp.toPx()),
                size = Size(4.dp.toPx(), size.height + 4.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.3f),
                topLeft = Offset(thumbX - 3.dp.toPx(), -3.dp.toPx()),
                size = Size(6.dp.toPx(), size.height + 6.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}
