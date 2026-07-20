package com.example.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.*

// ── Light mode palette ──────────────────────────────────────────────────────
private val BgMain      = Color(0xFFF6F8FC)
private val BgSoft      = Color(0xFFF0F3F9)
private val BgHighlight = Color(0xFFFAFBFF)

private val GlassEdge     = Color(0xD9FFFFFF)
private val ShadowSoft    = Color(0x0F0F172A)
private val InnerShadow   = Color(0x080F172A)

private val PrimaryBlue   = Color(0xFF3B82F6)
private val AccentCyan    = Color(0xFF06B6D4)
private val AccentViolet  = Color(0xFF8B5CF6)
private val AccentEmerald = Color(0xFF10B981)
private val AccentRose    = Color(0xFFF472B6)

// ── Pane model ──────────────────────────────────────────────────────────────
private data class GlassPane(
    val accent: Color,
    val heightDp: androidx.compose.ui.unit.Dp,
    val brightness: Float,       // 0.0–1.0, primary=1.0 secondary=0.85
    val sheenOffset: Float,      // different positions per pane
    val sheenWidth: Float        // different band widths per pane
)

private val leftTallPane = GlassPane(PrimaryBlue, 600.dp, 1.0f, 0.22f, 0.48f)

private val rightPanes = listOf(
    GlassPane(AccentCyan,    140.dp, 0.88f, 0.32f, 0.42f),
    GlassPane(AccentViolet,  140.dp, 0.85f, 0.18f, 0.50f),
    GlassPane(AccentEmerald, 140.dp, 0.82f, 0.38f, 0.40f),
    GlassPane(AccentRose,    140.dp, 0.80f, 0.26f, 0.45f),
)

// ── Main showcase ───────────────────────────────────────────────────────────
@Composable
fun GlassDashboardShowcase() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        // ── 1. Layered background gradient ──────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(BgHighlight, BgMain, BgSoft),
                        center = Offset(0.35f, 0.25f),
                        radius = 1.4f
                    )
                )
        )

        // ── 2. Atmospheric light diffusion ──────────────────────────
        // Large soft blob — upper left
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.Transparent
                        ),
                        center = Offset(0.22f, 0.28f),
                        radius = 0.52f
                    )
                )
        )
        // Smaller warm blob — right mid
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PrimaryBlue.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = Offset(0.75f, 0.55f),
                        radius = 0.35f
                    )
                )
        )
        // Soft lower-right ambient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AccentViolet.copy(alpha = 0.02f),
                            Color.Transparent
                        ),
                        center = Offset(0.85f, 0.80f),
                        radius = 0.30f
                    )
                )
        )

        // ── 3. Card grid (exact same layout) ────────────────────────
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
            ) {
                PremiumGlassPane(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    pane = leftTallPane
                )
            }

            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                rightPanes.forEach { pane ->
                    PremiumGlassPane(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(pane.heightDp),
                        pane = pane
                    )
                }
            }
        }
    }
}

// ── Premium real-glass pane ────────────────────────────────────────────────
@Composable
private fun PremiumGlassPane(
    modifier: Modifier = Modifier,
    pane: GlassPane
) {
    val shape = RoundedCornerShape(28.dp)
    val accent = pane.accent
    val bri = pane.brightness

    Box(
        modifier = modifier
            // ── 1. Multi-layered floating shadow ──────────────────
            .drawBehind {
                val r = 28.dp.toPx()
                // Layer 1: wide, very soft
                drawRoundRect(
                    color = ShadowSoft.copy(alpha = 0.05f * bri),
                    topLeft = Offset(-4.dp.toPx(), 6.dp.toPx()),
                    size = Size(size.width + 8.dp.toPx(), size.height + 4.dp.toPx()),
                    cornerRadius = CornerRadius(r + 4.dp.toPx()),
                    style = Fill
                )
                // Layer 2: medium, close
                drawRoundRect(
                    color = ShadowSoft.copy(alpha = 0.04f * bri),
                    topLeft = Offset(0f, 8.dp.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(r),
                    style = Fill
                )
                // Layer 3: tight, heavier at bottom
                drawRoundRect(
                    color = ShadowSoft.copy(alpha = 0.03f * bri),
                    topLeft = Offset(2.dp.toPx(), 12.dp.toPx()),
                    size = Size(size.width - 4.dp.toPx(), size.height - 2.dp.toPx()),
                    cornerRadius = CornerRadius(r),
                    style = Fill
                )
            }
            .clip(shape)
    ) {
        // ── 2. Layered glass surface (natural frosted glass) ─────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.92f * bri),
                            Color.White.copy(alpha = 0.68f * bri),
                            Color(0xFFF4F7FC).copy(alpha = 0.55f * bri),
                            Color(0xFFEFF2F8).copy(alpha = 0.45f * bri)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(1f, 1f)
                    )
                )
        )

        // ── 3. Refracted accent light (coloured light passing
        //       through glass — Accent → Soft White → Transparent
        //       → Soft White → Accent) ────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.04f * bri),
                            Color.White.copy(alpha = 0.03f * bri),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.02f * bri),
                            accent.copy(alpha = 0.03f * bri)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(1f, 1f)
                    )
                )
        )

        // ── 4. Multi-layer optical border (glass thickness) ─────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val r = 28.dp.toPx()
                    onDrawBehind {
                        // Layer 1: soft outer white glow
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.15f * bri),
                            cornerRadius = CornerRadius(r),
                            style = Stroke(width = 3.dp.toPx())
                        )
                        // Layer 2: thin translucent edge
                        drawRoundRect(
                            color = GlassEdge.copy(alpha = 0.60f * bri),
                            cornerRadius = CornerRadius(r),
                            style = Stroke(width = 1.2f.dp.toPx())
                        )
                        // Layer 3: bright inner highlight (polished edge)
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.50f * bri),
                            cornerRadius = CornerRadius(r),
                            style = Stroke(width = 0.6f.dp.toPx())
                        )
                    }
                }
        )

        // ── 5. Very soft inner shadow (depth) ────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val r = 28.dp.toPx()
                    onDrawBehind {
                        drawRoundRect(
                            color = InnerShadow.copy(alpha = 0.04f * bri),
                            topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                            size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                            cornerRadius = CornerRadius(r),
                            style = Stroke(width = 1.5f.dp.toPx())
                        )
                    }
                }
        )

        // ── 6. Fresnel edge lighting (polished glass edges) ─────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val r = 28.dp.toPx()
                    val fresnelPath = Path().apply {
                        // Top edge highlight (full width)
                        moveTo(r, 0f)
                        lineTo(size.width - r, 0f)
                        arcTo(
                            rect = androidx.compose.ui.geometry.Rect(
                                size.width - r * 2, 0f,
                                size.width, r * 2
                            ),
                            startAngleDegrees = -90f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false
                        )
                        lineTo(size.width, r)
                        lineTo(size.width, 0f)
                        close()
                    }
                    onDrawBehind {
                        drawPath(
                            path = fresnelPath,
                            color = Color.White.copy(alpha = 0.08f * bri),
                            style = Fill
                        )
                    }
                }
        )

        // ── 7. Primary curved reflection (top-left corner) ──────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val r = 28.dp.toPx()
                    val primaryReflection = Path().apply {
                        moveTo(r, 0f)
                        cubicTo(r * 0.4f, 0f, 0f, r * 0.4f, 0f, r)
                        lineTo(0f, 0f)
                        close()
                        // Top edge strip
                        moveTo(r, 0f)
                        lineTo(size.width * 0.50f, 0f)
                        lineTo(size.width * 0.50f, 1.8f.dp.toPx())
                        lineTo(r, 1.8f.dp.toPx())
                        close()
                        // Left edge strip
                        moveTo(0f, r)
                        lineTo(0f, size.height * 0.45f)
                        lineTo(1.8f.dp.toPx(), size.height * 0.45f)
                        lineTo(1.8f.dp.toPx(), r)
                        close()
                    }
                    onDrawBehind {
                        drawPath(
                            path = primaryReflection,
                            color = Color.White.copy(alpha = 0.50f * bri),
                            style = Fill
                        )
                    }
                }
        )

        // ── 8. Secondary shorter reflection (offset inward) ─────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val r = 28.dp.toPx()
                    val secondaryReflection = Path().apply {
                        val inset = 4.dp.toPx()
                        // Shorter top strip
                        moveTo(r + inset, inset)
                        lineTo(size.width * 0.30f, inset)
                        lineTo(size.width * 0.30f, inset + 0.8f.dp.toPx())
                        lineTo(r + inset, inset + 0.8f.dp.toPx())
                        close()
                        // Shorter left strip
                        moveTo(inset, r + inset)
                        lineTo(inset, size.height * 0.28f)
                        lineTo(inset + 0.8f.dp.toPx(), size.height * 0.28f)
                        lineTo(inset + 0.8f.dp.toPx(), r + inset)
                        close()
                    }
                    onDrawBehind {
                        drawPath(
                            path = secondaryReflection,
                            color = Color.White.copy(alpha = 0.20f * bri),
                            style = Fill
                        )
                    }
                }
        )

        // ── 9. Tiny corner sparkle ──────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val r = 28.dp.toPx()
                    val sparklePosX = r * 0.35f
                    val sparklePosY = r * 0.35f
                    val sparklePath = Path().apply {
                        // Small arc at the very corner
                        arcTo(
                            rect = androidx.compose.ui.geometry.Rect(0f, 0f, r * 0.7f, r * 0.7f),
                            startAngleDegrees = 0f,
                            sweepAngleDegrees = 40f,
                            forceMoveTo = false
                        )
                    }
                    onDrawBehind {
                        drawPath(
                            path = sparklePath,
                            color = Color.White.copy(alpha = 0.35f * bri),
                            style = Stroke(width = 1.2f.dp.toPx())
                        )
                    }
                }
        )

        // ── 10. Diagonal glass sheen (varied per pane) ──────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val sheenOff = pane.sheenOffset
                    val sheenWid = pane.sheenWidth
                    onDrawBehind {
                        val diag = hypot(
                            size.width.toDouble(),
                            size.height.toDouble()
                        ).toFloat()
                        val band = diag * sheenWid
                        val off = diag * sheenOff

                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0f),
                                    Color.White.copy(alpha = 0.06f * bri),
                                    Color.White.copy(alpha = 0.15f * bri),
                                    Color.White.copy(alpha = 0.06f * bri),
                                    Color.White.copy(alpha = 0f)
                                ),
                                start = Offset(off * 0.3f, off * 0.3f),
                                end = Offset(
                                    off * 0.3f + band * 0.5f,
                                    off * 0.3f + band * 0.5f
                                )
                            ),
                            cornerRadius = CornerRadius(28.dp.toPx()),
                            style = Fill
                        )
                    }
                }
        )

        // ── 11. Bottom-right subtle depth ───────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            InnerShadow.copy(alpha = 0.04f * bri),
                            Color.Transparent
                        ),
                        center = Offset(1f, 1f),
                        radius = 0.50f
                    )
                )
        )
    }
}
