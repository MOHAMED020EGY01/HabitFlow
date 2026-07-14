package com.example.widget.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF

/**
 * Renders a dashed full circle outline as a Bitmap for empty widget slots,
 * matching the footprint of the circular progress ring.
 */
object EmptySlotCircleRenderer {

    fun render(
        context: Context,
        sizePx: Int,
        strokeWidthPx: Float,
        dashLengthPx: Float = 15f,
        gapLengthPx: Float = 10f
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokeWidthPx
            strokeCap = Paint.Cap.ROUND
            color = android.graphics.Color.argb(46, 255, 255, 255) // ~18% white
            pathEffect = DashPathEffect(floatArrayOf(dashLengthPx, gapLengthPx), 0f)
        }

        val padding = strokeWidthPx / 2 + 4f
        val rectF = RectF(
            padding,
            padding,
            sizePx.toFloat() - padding,
            sizePx.toFloat() - padding
        )

        canvas.drawOval(rectF, paint)

        return bitmap
    }
}
