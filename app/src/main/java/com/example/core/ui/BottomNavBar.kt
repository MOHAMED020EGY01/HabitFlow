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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
            .height(80.dp)
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
    val glassSurfaceColor = MaterialTheme.colorScheme.surface
    val glassBackgroundColor = MaterialTheme.colorScheme.background
    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

    val glassBrush = remember(isDark, animationsEnabled, glassSurfaceColor, glassBackgroundColor) {
        val topColor = glassSurfaceColor.copy(alpha = if (animationsEnabled) 0.85f else 0.95f)
        val bottomColor = glassBackgroundColor.copy(alpha = if (animationsEnabled) 0.70f else 0.90f)
        Brush.verticalGradient(listOf(topColor, bottomColor))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(12.dp, shape, clip = false)
            .background(glassBrush, shape)
            .then(if (animationsEnabled && blurCapable) Modifier.blur(16.dp) else Modifier)
            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), shape)
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
    val pillColor = MaterialTheme.colorScheme.primary
    val layoutDirection = LocalLayoutDirection.current
    
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(tabWidth)
            .graphicsLayer {
                val isRtl = layoutDirection == LayoutDirection.Rtl
                val multiplier = if (isRtl) -1f else 1f
                translationX = multiplier * engine.progress * size.width
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(58.dp)
                .shadow(
                    elevation = if (animationsEnabled) 6.dp else 0.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = pillColor.copy(alpha = 0.35f),
                    spotColor = pillColor.copy(alpha = 0.45f)
                )
                .background(
                    color = pillColor.copy(alpha = if (isDark) 0.65f else 0.55f),
                    shape = RoundedCornerShape(16.dp)
                )
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
        MainTab.entries.forEach { tab ->
            BottomNavigationItem(
                tab = tab,
                isSelected = tab == engine.targetTab,
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
    isSelected: Boolean,
    animationsEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animSpec = if (animationsEnabled) tween<Float>(400, easing = FastOutSlowInEasing) else snap()
    val colorSpec = if (animationsEnabled) tween<Color>(400, easing = FastOutSlowInEasing) else snap()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else if (isSelected) 1.15f else 1.0f,
        animationSpec = if (isPressed) snap() else animSpec,
        label = "TabScale"
    )

    val tint by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                     else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        animationSpec = colorSpec,
        label = "TabTint"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .testTag("nav_item_${tab.name}"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(tab.titleRes),
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
            )
        }
    }
}
