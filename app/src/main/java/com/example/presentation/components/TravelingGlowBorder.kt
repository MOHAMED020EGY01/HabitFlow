package com.example.presentation.components

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.util.DeviceCapability

/**
 * Reads the existing device-capability system and glass-effect setting
 * to derive a device tier string.
 * (Retained from Part 90 — the travelingGlowBorder modifier itself was
 * removed in Part 93; only this helper survives.)
 */
@Composable
fun rememberDeviceTier(): String {
    val context = LocalContext.current
    val glassEffectMode = LocalGlassEffectMode.current
    val deviceBlurCapable = remember(context) { DeviceCapability.isBlurCapableDevice() }

    return remember(glassEffectMode, deviceBlurCapable) {
        when (glassEffectMode) {
            1 -> "HIGH"
            2 -> "LOW"
            else -> if (deviceBlurCapable) "MID" else "LOW"
        }
    }
}
