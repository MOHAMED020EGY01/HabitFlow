package com.example.core.infrastructure.widget.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

/**
 * Renders a small colored checkmark icon as a Bitmap for Glance widgets.
 */
object CheckIconRenderer {

    fun render(
        context: Context,
        colorHex: String,
        sizePx: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val accentColor = try {
            android.graphics.Color.parseColor(colorHex)
        } catch (_: Exception) {
            android.graphics.Color.WHITE
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            strokeWidth = sizePx / 6f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val path = android.graphics.Path().apply {
            moveTo(sizePx * 0.25f, sizePx * 0.5f)
            lineTo(sizePx * 0.45f, sizePx * 0.7f)
            lineTo(sizePx * 0.75f, sizePx * 0.3f)
        }

        canvas.drawPath(path, paint)

        return bitmap
    }
}
