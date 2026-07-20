package com.example.core.infrastructure.widget.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF

/**
 * Renders a half-circle (semi-circle) progress ring as a Bitmap,
 * since Glance widgets cannot draw arbitrary custom shapes natively.
 */
object HalfCircleProgressRenderer {

    fun render(
        context: Context,
        progressPercent: Float,    // 0f..100f
        colorHex: String,
        widthPx: Int = 200,
        heightPx: Int = 110,
        strokeWidthPx: Float = 24f
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        val accentColor = try {
            android.graphics.Color.parseColor(colorHex)
        } catch (_: Exception) {
            android.graphics.Color.parseColor("#7C4DFF")
        }

        val padding = strokeWidthPx / 2 + 4f
        val rectF = RectF(
            padding,
            padding,
            widthPx - padding,
            widthPx - (strokeWidthPx / 2) // Bottom adjustment to keep it semi-circular
        )

        // Background track (full semicircle, faint semi-transparent white)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            strokeCap = Paint.Cap.ROUND
            color = android.graphics.Color.argb(40, 255, 255, 255) // ~15% white
        }
        // Draw from 180 to 180 (top half)
        canvas.drawArc(rectF, 180f, 180f, false, trackPaint)

        // Progress arc (colored, percentage)
        val sweepAngle = 180f * (progressPercent.coerceIn(0f, 100f) / 100f)
        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            strokeCap = Paint.Cap.ROUND
            color = accentColor
        }
        canvas.drawArc(rectF, 180f, sweepAngle, false, progressPaint)

        return bitmap
    }
}
