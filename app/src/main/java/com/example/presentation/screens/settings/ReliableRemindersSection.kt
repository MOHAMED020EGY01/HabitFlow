package com.example.presentation.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.service.HabitBackgroundService
import com.example.util.BackgroundReliabilityHelper

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ReliableRemindersSection(
    viewModel: SettingsViewModel,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isServiceEnabled = uiState.isBackgroundServiceEnabled

    // State for permissions
    var hasNotification by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var hasOverlay by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    var hasBatteryExemption by remember {
        mutableStateOf(BackgroundReliabilityHelper.isIgnoringBatteryOptimizations(context))
    }

    // Dynamic listener to update states on ON_RESUME
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlay = Settings.canDrawOverlays(context)
                hasBatteryExemption = BackgroundReliabilityHelper.isIgnoringBatteryOptimizations(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotification = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }

                // Rule: Automatically enable background service when overlay is granted
                if (hasOverlay && !isServiceEnabled) {
                    viewModel.setBackgroundServiceEnabled(true)
                    HabitBackgroundService.start(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Permission launcher for Notification (Tiramisu+)
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotification = granted
    }

    // Progress calculation
    val totalSteps = 3
    val stepsDone = listOf(hasNotification, hasOverlay, hasBatteryExemption).count { it }
    val isFullyProtected = stepsDone == totalSteps

    com.example.presentation.components.GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reliable_reminders_card"),
        shape = RoundedCornerShape(16.dp),
        habitColor = if (isFullyProtected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.reliable_reminders_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isFullyProtected) {
                            if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF69F0AE) else Color(0xFF1B5E20)
                        } else MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (isFullyProtected) {
                            stringResource(R.string.reliable_reminders_protected)
                        } else {
                            stringResource(R.string.reliable_reminders_setup_needed, stepsDone, totalSteps)
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isFullyProtected) {
                            if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF69F0AE) else Color(0xFF1B5E20)
                        } else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Check or Warning Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isFullyProtected) {
                                if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF69F0AE).copy(alpha = 0.15f) else Color(0xFF1B5E20).copy(alpha = 0.15f)
                            } else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFullyProtected) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isFullyProtected) {
                            if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF69F0AE) else Color(0xFF1B5E20)
                        } else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Step Progress Track
            if (!isFullyProtected) {
                LinearProgressIndicator(
                    progress = { stepsDone.toFloat() / totalSteps.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                )
            }

            // Detailed steps breakdown (Animated visibility)
            AnimatedVisibility(
                visible = !isFullyProtected,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Step 1: Notifications
                    PermissionStepRow(
                        title = stringResource(R.string.notifications),
                        isGranted = hasNotification,
                        isArabic = isArabic
                    )

                    // Step 2: Overlay
                    PermissionStepRow(
                        title = stringResource(R.string.display_over_apps),
                        isGranted = hasOverlay,
                        isArabic = isArabic
                    )

                    // Step 3: Battery Optimization
                    PermissionStepRow(
                        title = stringResource(R.string.battery_optimization),
                        isGranted = hasBatteryExemption,
                        isArabic = isArabic
                    )
                }
            }

            // Interactive Sequential Fix Action
            if (!isFullyProtected) {
                Button(
                    onClick = {
                        when {
                            !hasNotification -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                            !hasOverlay -> {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                                // Pre-enable background service
                                viewModel.setBackgroundServiceEnabled(true)
                                HabitBackgroundService.start(context)
                            }
                            !hasBatteryExemption -> {
                                BackgroundReliabilityHelper.requestIgnoreBatteryOptimizations(context)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("reliable_reminders_fix_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.reliable_reminders_fix_now),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Manufacturer Auto-start fallback link if needed
            val manufacturer = Build.MANUFACTURER.lowercase()
            if (!isFullyProtected && manufacturer in listOf("xiaomi", "huawei", "honor", "oppo", "vivo", "samsung")) {
                TextButton(
                    onClick = { BackgroundReliabilityHelper.openManufacturerAutostartSettings(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("autostart_link_button")
                ) {
                    Text(
                        text = stringResource(R.string.enable_autostart, Build.MANUFACTURER),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionStepRow(
    title: String,
    isGranted: Boolean,
    isArabic: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = if (isGranted) {
                if (isArabic) "مسموح ✓" else "Allowed ✓"
            } else {
                if (isArabic) "مطلوب" else "Required"
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isGranted) Color(0xFF69F0AE) else Color(0xFFFF5252)
        )
    }
}
