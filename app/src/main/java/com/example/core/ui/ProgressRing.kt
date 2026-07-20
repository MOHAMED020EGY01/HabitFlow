package com.example.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProgressRing(
    progress: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    strokeWidth: Dp = 10.dp,
    gradientColors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary
    ),
    textColor: Color = MaterialTheme.colorScheme.primary,
    textSize: androidx.compose.ui.unit.TextUnit = 24.sp,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        val strokeColor = backgroundColor
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidthPx = strokeWidth.toPx()
            val diameter = this.size.width - strokeWidthPx
            val radius = diameter / 2f

            // Background Circle
            drawCircle(
                color = strokeColor,
                radius = radius,
                style = Stroke(width = strokeWidthPx)
            )

            // Progress Arc (only if progress > 0)
            if (animatedProgress > 0f) {
                if (animatedProgress >= 0.999f) {
                    // Draw a perfect circle at 100% to avoid any rounded cap overlaps or seams
                    drawCircle(
                        brush = Brush.sweepGradient(gradientColors),
                        radius = radius,
                        style = Stroke(width = strokeWidthPx)
                    )
                } else {
                    // Draw the arc with the exact same radius and bounding box
                    drawArc(
                        brush = Brush.sweepGradient(gradientColors),
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(strokeWidthPx / 2f, strokeWidthPx / 2f),
                        size = androidx.compose.ui.geometry.Size(diameter, diameter),
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Percentage Text
        Text(
            text = com.example.core.util.AppFormatters.forceWesternDigits("${(progress * 100).toInt()}%"),
            fontSize = textSize,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
