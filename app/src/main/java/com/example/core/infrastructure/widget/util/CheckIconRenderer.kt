package com.example.core.infrastructure.widget.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

import android.util.Log

/**
 * Renders a small colored checkmark icon as a Bitmap for Glance widgets.
 */
object CheckIconRenderer {
    private const val TAG = "CheckIconRenderer_DIAG"

    fun render(
        context: Context,
        colorHex: String,
        sizePx: Int
    ): Bitmap {
        Log.d(TAG, "render starting | size: $sizePx | color: $colorHex")

        val bitmap = try {
            Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            Log.e(TAG, "Bitmap.createBitmap FAILED | size: $sizePx", e)
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
        val canvas = Canvas(bitmap)

        try {
            val accentColor = try {
                android.graphics.Color.parseColor(colorHex)
            } catch (e: Exception) {
                Log.e(TAG, "Color.parseColor FAILED for $colorHex", e)
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
            Log.d(TAG, "render success")
        } catch (e: Exception) {
            Log.e(TAG, "Canvas drawing FAILED", e)
        }

        return bitmap
    }
}
