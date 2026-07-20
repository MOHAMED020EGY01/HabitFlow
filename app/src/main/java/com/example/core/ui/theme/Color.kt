package com.example.core.ui.theme

import androidx.compose.ui.graphics.Color

// ── Dark Mode (UNCHANGED — must remain byte-for-byte identical) ──────────
val PrimaryDark = Color(0xFF7C4DFF)
val SecondaryDark = Color(0xFF00E5FF)
val BackgroundDark = Color(0xFF0F0F1A)
val SurfaceDark = Color(0xFF1A1A2E)
val OnPrimaryDark = Color(0xFFFFFFFF)
val OnBackgroundDark = Color(0xFFE8E8F0)
val OnSurfaceDark = Color(0xFFE8E8F0)

// ── Light Mode (updated Part 91) ────────────────────────────────────────
val PrimaryLight = Color(0xFF5E35B1)   // UNCHANGED — stays purple
val SecondaryLight = Color(0xFF00B8D4)  // UNCHANGED

// Background: gradient bottom (flat fallback for scheme)
val BackgroundLight = Color(0xFFD8E6D0)        // soft sage green (bottom of gradient)
val SurfaceLight = Color(0xFFFCFDF9)            // cream-white card surface

val OnPrimaryLight = Color(0xFFFFFFFF)           // UNCHANGED
val OnBackgroundLight = Color(0xFF1F2E20)        // dark charcoal-green (was #1A1A2E)
val OnSurfaceLight = Color(0xFF1F2E20)           // dark charcoal-green (was #1A1A2E)

val LightOnSurfaceVariant = Color(0xFF5B6B57)    // muted sage-gray for secondary text

// ── Gradient endpoints (Light Mode only) ─────────────────────────────────
val LightBackgroundGradientTop = Color(0xFFF3F7EF)     // near-white with green tint
val LightBackgroundGradientBottom = Color(0xFFD8E6D0)  // soft sage green

// ── Shared / Error (both themes) ────────────────────────────────────────
val ErrorColor = Color(0xFFFF5252)
val SuccessColor = Color(0xFF69F0AE)
