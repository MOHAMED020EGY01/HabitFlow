package com.example.core.util

import android.opengl.GLES20
import android.os.Build
import java.util.Locale

/**
 * Device-tier heuristic for GPU-intensive visual effects like gaussian blur.
 *
 * ## Heuristic logic
 * 1. **API gate:** `Build.VERSION.SDK_INT >= 31` (Android 12) is required because
 *    `Modifier.blur()` in Compose uses `android.graphics.RenderEffect` internally,
 *    which only exists from API 31 onward.
 * 2. **GPU blacklist:** On API 31+ devices, we attempt to read `GL_RENDERER` via
 *    `GLES20.glGetString()`. If the renderer matches known low-end patterns, blur
 *    is disabled because those GPUs lack native hardware blur support and fall
 *    back to expensive software-emulated convolution.
 *
 * ## Blacklisted GPU patterns
 * - **Mali-G5x / Mali-G3x / Mali-4xx** — Mid-range and old Mali GPUs that
 *   typically lack dedicated blur hardware; the driver falls back to compute or
 *   CPU-assisted blur (see GPU_RENDERING_AUDIT.md §6 🔴#1).
 * - **PowerVR Rogue** — Common in MediaTek Helio P-series; no hardware blur.
 * - **Adreno 5xx and below** — Older Qualcomm; `RenderEffect` blur is slow.
 * - **Generic software renderers** — Emulators, `android.graphics` sw renderer.
 *
 * ## Behavior on target device (Realme C53)
 * - Build: API 33 (Android 13).
 * - SoC: Unisoc T612.
 * - GPU: Mali-G57 (matches `Mali-G5` pattern).
 * - **Result: `isBlurCapableDevice()` returns `false`** → blur disabled by default.
 *
 * ## Override
 * The user can override this auto-detection via Settings → Glass Effect
 * (see [com.example.feature.settings.presentation.SettingsScreen]).
 */
object DeviceCapability {

    private var _glRendererChecked = false
    private var _glRenderer: String? = null

    /**
     * Eager cache of the GL_RENDERER string (may be called from any thread).
     * If no GL context is available (e.g. during early startup), returns null
     * and [isBlurCapableDevice] will conservatively return false.
     */
    private fun readGlRenderer(): String? {
        if (!_glRendererChecked) {
            try {
                // GLES20.glGetString requires a current GL context.
                // On Compose + hardware acceleration this is safe after the
                // first frame, but during early composition it may return null.
                _glRenderer = GLES20.glGetString(GLES20.GL_RENDERER)
            } catch (_: Exception) {
                _glRenderer = null
            }
            _glRendererChecked = true
        }
        return _glRenderer
    }

    /**
     * Returns `true` if the device is likely capable of hardware-accelerated
     * `Modifier.blur()` without significant frame-time impact.
     *
     * This is a **conservative heuristic**: it favours disabling blur on
     * uncertain devices to avoid the severe jank documented in Part 73/75.
     */
    fun isBlurCapableDevice(): Boolean {
        // API gate: RenderEffect is only available from API 31+.
        if (Build.VERSION.SDK_INT < 31) return false

        // Check GL renderer for known low-end patterns.
        val renderer = readGlRenderer()?.lowercase(Locale.ROOT) ?: return false

        // Known problematic GPU patterns:
        val lowEndPatterns = listOf(
            // Mali series without native blur support
            "mali-g5", "mali-g3", "mali-4", "mali-3",
            // PowerVR Rogue (common in MediaTek Helio P)
            "powervr", "rogue",
            // Older Adreno
            "adreno 5", "adreno 4", "adreno 3",
            // Software / emulated renderers
            "software", "swiftshader", "llvmpipe"
        )

        for (pattern in lowEndPatterns) {
            if (renderer.contains(pattern)) return false
        }

        // Also check SoC/hardware name for Unisoc/Spreadtrum (common low-end).
        val hardware = Build.HARDWARE.lowercase(Locale.ROOT)
        val board = Build.BOARD.lowercase(Locale.ROOT)
        if (hardware.contains("unisoc") || hardware.contains("sprd") ||
            board.contains("unisoc") || board.contains("sprd")) {
            return false
        }

        return true
    }
}
