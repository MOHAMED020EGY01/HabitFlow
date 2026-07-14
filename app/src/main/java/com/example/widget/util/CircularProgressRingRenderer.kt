package com.example.widget.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/**
 * Renders a FULL 360° circular progress ring as a Bitmap.
 * Corrective implementation for the final approved widget design.
 */
object CircularProgressRingRenderer {

    fun render(
        context: Context,
        progressPercent: Float,    // 0f..100f
        colorHex: String,
        sizePx: Int = 150,
        strokeWidthPx: Float = 12f
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val accentColor = try {
            android.graphics.Color.parseColor(colorHex)
        } catch (_: Exception) {
            android.graphics.Color.parseColor("#7C4DFF")
        }

        val padding = strokeWidthPx / 2 + 4f
        val rectF = RectF(
            padding,
            padding,
            sizePx.toFloat() - padding,
            sizePx.toFloat() - padding
        )

        // 1. Full circular track (faint white)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            strokeCap = Paint.Cap.ROUND
            color = android.graphics.Color.argb(30, 255, 255, 255) // ~12% white
        }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, (sizePx - 2 * padding) / 2f, trackPaint)

        // 2. Progress arc (colored, percentage)
        // startAngle = -90f (12 o'clock)
        val sweepAngle = 360f * (progressPercent.coerceIn(0f, 100f) / 100f)
        if (sweepAngle > 0f) {
            val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = strokeWidthPx
                strokeCap = Paint.Cap.ROUND
                color = accentColor
            }
            canvas.drawArc(rectF, -90f, sweepAngle, false, progressPaint)
        }

        return bitmap
    }
}
