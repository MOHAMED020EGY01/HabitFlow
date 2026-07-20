package com.example.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.core.navigation.MainTab
import com.example.core.navigation.NavigationMotionEngine

@Composable
fun BottomNavBar(
    engine: NavigationMotionEngine,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    navBarPaddingRequired: Boolean = true
) {
    if (!visible) return

    val isDark = isSystemInDarkTheme()
    val animationsEnabled = LocalCardAnimationsEnabled.current
    val bottomNavBlurCapable = remember { com.example.core.util.DeviceCapability.isBlurCapableDevice() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (navBarPaddingRequired) Modifier.navigationBarsPadding() else Modifier)
            .padding(start = 24.dp, end = 24.dp, bottom = 18.dp) // Floating margin
            .height(64.dp), // Compact height
        contentAlignment = Alignment.Center
    ) {
        BottomBarBackground(isDark, animationsEnabled, bottomNavBlurCapable)

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val tabWidth = maxWidth / MainTab.entries.size

            SlidingIndicator(
                engine = engine,
                tabWidth = tabWidth,
                isDark = isDark,
                animationsEnabled = animationsEnabled
            )

            BottomNavigationRow(
                engine = engine,
                animationsEnabled = animationsEnabled
            )
        }
    }
}

@Composable
private fun BottomBarBackground(
    isDark: Boolean,
    animationsEnabled: Boolean,
    blurCapable: Boolean
) {
    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val glassSurfaceColor = MaterialTheme.colorScheme.surface
    val glassBackgroundColor = MaterialTheme.colorScheme.background
    val shape = CircleShape

    val glassBrush = remember(isDark, animationsEnabled, glassSurfaceColor, glassBackgroundColor) {
        val topColor = glassSurfaceColor.copy(alpha = if (animationsEnabled) 0.85f else 0.95f)
        val bottomColor = glassBackgroundColor.copy(alpha = if (animationsEnabled) 0.70f else 0.90f)
        Brush.verticalGradient(listOf(topColor, bottomColor))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(
                elevation = 16.dp, 
                shape = shape, 
                clip = false, 
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), 
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
            .background(glassBrush, shape)
            .then(if (animationsEnabled && blurCapable) Modifier.blur(12.dp) else Modifier)
            .border(1.dp, outlineColor, shape)
            .clip(shape)
    )
}

@Composable
private fun SlidingIndicator(
    engine: NavigationMotionEngine,
    tabWidth: androidx.compose.ui.unit.Dp,
    isDark: Boolean,
    animationsEnabled: Boolean
) {
    val pillColor = MaterialTheme.colorScheme.primaryContainer
    val layoutDirection = LocalLayoutDirection.current
    
    // Interaction pulse and movement glow
    val movementAlpha by animateFloatAsState(
        targetValue = if (engine.isAnimating) 0.6f else 0.25f,
        animationSpec = tween(300),
        label = "MovementAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(tabWidth)
            .graphicsLayer {
                val isRtl = layoutDirection == LayoutDirection.Rtl
                val multiplier = if (isRtl) -1f else 1f
                translationX = multiplier * engine.progress * size.width
            }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    if (animationsEnabled) {
                        // Dynamic soft manual glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(pillColor.copy(alpha = movementAlpha), Color.Transparent),
                                center = center,
                                radius = size.maxDimension * 0.9f
                            ),
                            radius = size.maxDimension * 0.9f
                        )
                    }
                }
                .background(pillColor, CircleShape)
        )
    }
}

@Composable
private fun BottomNavigationRow(
    engine: NavigationMotionEngine,
    animationsEnabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MainTab.entries.forEachIndexed { index, tab ->
            // Calculate interpolation progress for this specific tab
            val tabProgress = remember(engine.progress) {
                // Returns 1.0 if the pager is exactly on this index, 0.0 if away.
                // It creates a smooth ramp as the user swipes.
                (1f - kotlin.math.abs(engine.progress - index)).coerceIn(0f, 1f)
            }

            BottomNavigationItem(
                tab = tab,
                progress = tabProgress,
                animationsEnabled = animationsEnabled,
                onClick = { engine.navigateTo(tab) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomNavigationItem(
    tab: MainTab,
    progress: Float,
    animationsEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val activeColor = MaterialTheme.colorScheme.onPrimaryContainer
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    // Linear color interpolation based on shared engine progress
    val tint = remember(progress, activeColor, inactiveColor) {
        androidx.compose.ui.graphics.lerp(inactiveColor, activeColor, progress)
    }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = if (isPressed) snap() else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "PressScale"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .testTag("nav_item_${tab.name}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (progress > 0.5f) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        // Unify scaling: Combine manual press scale with progress-based selection scale
                        val selectionScale = 1f + (0.04f * progress)
                        val finalScale = pressScale * selectionScale
                        scaleX = finalScale
                        scaleY = finalScale
                    }
            )
            
            Text(
                text = stringResource(tab.titleRes),
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer { 
                    // Smooth alpha and scale transition perfectly synced with progress
                    alpha = progress
                    val selectionScale = 0.9f + (0.1f * progress)
                    val finalScale = pressScale * selectionScale
                    scaleX = finalScale
                    scaleY = finalScale
                }
            )
        }
    }
}
