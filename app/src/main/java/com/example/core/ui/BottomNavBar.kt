package com.example.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.core.navigation.Routes
import com.example.core.navigation.navigateToTopLevelDestination

@Immutable
data class BottomNavItem(
    val titleRes: Int,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(
        com.example.R.string.menu_home,
        Routes.HOME,
        Icons.Filled.Home,
        Icons.Outlined.Home
    ),
    BottomNavItem(
        com.example.R.string.menu_all_habits,
        Routes.ALL_HABITS,
        Icons.Filled.List,
        Icons.Outlined.List
    ),
    BottomNavItem(
        com.example.R.string.menu_summary,
        Routes.SUMMARY,
        Icons.Filled.Star,
        Icons.Outlined.Star
    ),
    BottomNavItem(
        com.example.R.string.menu_settings,
        Routes.SETTINGS,
        Icons.Filled.Settings,
        Icons.Outlined.Settings
    )
)

@Composable
fun BottomNavBar(
    selectedRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    navBarPaddingRequired: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val animationsEnabled = LocalCardAnimationsEnabled.current
    val bottomNavBlurCapable = remember { com.example.core.util.DeviceCapability.isBlurCapableDevice() }

    // Glass frosted vertical gradient background brush — live from theme surface
    val glassSurfaceColor = MaterialTheme.colorScheme.surface
    val glassBackgroundColor = MaterialTheme.colorScheme.background
    val glassBrush = remember(isDark, animationsEnabled, glassSurfaceColor, glassBackgroundColor) {
        val topColor = glassSurfaceColor.copy(alpha = if (animationsEnabled) 0.85f else 0.95f)
        val bottomColor = glassBackgroundColor.copy(alpha = if (animationsEnabled) 0.70f else 0.90f)
        Brush.verticalGradient(colors = listOf(topColor, bottomColor))
    }

// Border Sweep Gradient Rotation Animation
    val infiniteTransition = rememberInfiniteTransition(label = "BorderRotationTransition")
    val rotationAngleState: State<Float> = if (animationsEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
                repeatMode = RepeatMode.Reverse
            ),
            label = "BorderRotationAngle"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    // Colors from the LIVE theme primary — rebuilt when primary changes
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryArgb = primaryColor.toArgb()
    val lighterArgb = androidx.compose.ui.graphics.Color(
        red = (primaryColor.red + 1f) / 2f,
        green = (primaryColor.green + 1f) / 2f,
        blue = (primaryColor.blue + 1f) / 2f,
        alpha = 1f
    ).toArgb()
    val darkerArgb = androidx.compose.ui.graphics.Color(
        red = (primaryColor.red * 0.3f).coerceIn(0f, 1f),
        green = (primaryColor.green * 0.3f).coerceIn(0f, 1f),
        blue = (primaryColor.blue * 0.3f).coerceIn(0f, 1f),
        alpha = 1f
    ).toArgb()

    // ShaderBrush for animated sweep gradient border — rebuilt when primary changes
    val sweepShaderHolder = remember(isDark, primaryColor) {
        object {
            var shader: android.graphics.Shader? = null
            var lastSize: androidx.compose.ui.geometry.Size? = null
            val matrix = android.graphics.Matrix()
        }
    }

    val sweepBrush = remember(isDark, primaryColor) {

        object : ShaderBrush() {
            override fun createShader(size: androidx.compose.ui.geometry.Size): android.graphics.Shader {
                if (sweepShaderHolder.shader == null || sweepShaderHolder.lastSize != size) {
                    sweepShaderHolder.shader = android.graphics.SweepGradient(
                        size.width / 2f,
                        size.height / 2f,
                        intArrayOf(primaryArgb, lighterArgb, darkerArgb, lighterArgb, primaryArgb),
                        null
                    )
                    sweepShaderHolder.lastSize = size
                }
                sweepShaderHolder.matrix.reset()
                sweepShaderHolder.matrix.postRotate(rotationAngleState.value, size.width / 2f, size.height / 2f)
                sweepShaderHolder.shader!!.setLocalMatrix(sweepShaderHolder.matrix)
                return sweepShaderHolder.shader!!
            }
        }
    }

    val borderBrush = if (animationsEnabled) {
        sweepBrush
    } else {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        )
    }

    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

    Box(
        modifier = modifier
            .then(
                if (visible) {
                    val paddingMod = if (navBarPaddingRequired)
                        Modifier.navigationBarsPadding()
                    else
                        Modifier
                    Modifier
                        .fillMaxWidth()
                        .then(paddingMod)
                        .shadow(
                            elevation = 12.dp,
                            shape = shape,
                            clip = false,
                            ambientColor = Color.Black.copy(alpha = if (isDark) 0.5f else 0.15f),
                            spotColor = Color.Black.copy(alpha = if (isDark) 0.6f else 0.25f)
                        )
                        .background(Color.Transparent)
                        .border(
                            width = 1.5.dp,
                            brush = borderBrush,
                            shape = shape
                        )
                        .clip(shape)
                } else {
                    Modifier
                        .height(0.dp)
                        .fillMaxWidth()
                        .graphicsLayer { alpha = 0f }
                }
            )
    ) {
        // 1. Frosted Glass Background layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(glassBrush)
                .then(
                    if (animationsEnabled && visible && bottomNavBlurCapable) Modifier.blur(16.dp) else Modifier
                )
        )

        // 2. Active Interactive Content is hosted in a sub-composable!
        Box(
            modifier = if (visible) Modifier else Modifier.height(0.dp)
        ) {
            BottomNavInteractiveContent(
                selectedRoute = selectedRoute,
                onNavigate = onNavigate,
                isDark = isDark,
                animationsEnabled = animationsEnabled
            )
        }
    }
}

@Composable
private fun BottomNavInteractiveContent(
    selectedRoute: String?,
    onNavigate: (String) -> Unit,
    isDark: Boolean,
    animationsEnabled: Boolean
) {
    val selectedIndex = remember(selectedRoute) {
        bottomNavItems.indexOfFirst { it.route == selectedRoute }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val containerWidth = maxWidth
        val tabWidth = containerWidth / 4

        // Position tracking for RTL-safe sliding pill
        val tabPositions = remember { mutableStateListOf(0f, 0f, 0f, 0f) }

        // Animate position using the measured X offset
        val targetX = if (selectedIndex != -1) tabPositions.getOrNull(selectedIndex) ?: 0f else 0f
        
        val floatAnimSpec = if (animationsEnabled) {
            tween<Float>(durationMillis = 480, easing = FastOutSlowInEasing)
        } else {
            snap()
        }

        val animatedX by animateFloatAsState(
            targetValue = targetX,
            animationSpec = floatAnimSpec,
            label = "SlidingPillX"
        )

        if (selectedIndex != -1) {
            SlidingPill(
                animatedX = animatedX,
                tabWidth = tabWidth,
                isDark = isDark,
                animationsEnabled = animationsEnabled
            )
        }

        // 3. Interactive content Row
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                val onClick = remember(item.route, onNavigate) { { onNavigate(item.route) } }
                BottomNavItemView(
                    item = item,
                    isSelected = isSelected,
                    animationsEnabled = animationsEnabled,
                    onClick = onClick,
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned { coordinates ->
                            val x = coordinates.positionInParent().x
                            if (index < tabPositions.size) {
                                tabPositions[index] = x
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun SlidingPill(
    animatedX: Float,
    tabWidth: androidx.compose.ui.unit.Dp,
    isDark: Boolean,
    animationsEnabled: Boolean
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .offset {
                        androidx.compose.ui.unit.IntOffset(x = animatedX.toInt(), y = 0)
                    }
                    .width(tabWidth)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                val pillColor = MaterialTheme.colorScheme.primary
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
    }
}

@Composable
private fun RowScope.BottomNavItemView(
    item: BottomNavItem,
    isSelected: Boolean,
    animationsEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTint = MaterialTheme.colorScheme.onPrimary
    val isDark = isSystemInDarkTheme()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val unselectedTint = onSurfaceColor.copy(alpha = 0.45f)

    val floatAnimSpec = if (animationsEnabled) {
        tween<Float>(durationMillis = 480, easing = FastOutSlowInEasing)
    } else {
        snap()
    }

    val colorAnimSpec = if (animationsEnabled) {
        tween<Color>(durationMillis = 480, easing = FastOutSlowInEasing)
    } else {
        snap()
    }

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = floatAnimSpec,
        label = "IconScale_${item.route}"
    )

    val tint by animateColorAsState(
        targetValue = if (isSelected) selectedTint else unselectedTint,
        animationSpec = colorAnimSpec,
        label = "IconTint_${item.route}"
    )

    Box(
        modifier = modifier
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(72.dp)
                .height(58.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .testTag("nav_item_${item.route}"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(item.titleRes),
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
