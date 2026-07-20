package com.example.core.infrastructure.widget.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF

import android.util.Log

/**
 * Renders a dashed full circle outline as a Bitmap for empty widget slots,
 * matching the footprint of the circular progress ring.
 */
object EmptySlotCircleRenderer {
    private const val TAG = "EmptySlotRenderer_DIAG"

    fun render(
        context: Context,
        sizePx: Int,
        strokeWidthPx: Float,
        dashLengthPx: Float = 15f,
        gapLengthPx: Float = 10f
    ): Bitmap {
        Log.d(TAG, "render starting | size: $sizePx")
        
        val bitmap = try {
            Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            Log.e(TAG, "Bitmap.createBitmap FAILED | size: $sizePx", e)
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
        val canvas = Canvas(bitmap)

        try {
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
            Log.d(TAG, "render success")
        } catch (e: Exception) {
            Log.e(TAG, "Canvas drawing FAILED", e)
        }

        return bitmap
    }
}
