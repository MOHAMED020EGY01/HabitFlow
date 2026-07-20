package com.example.feature.home.presentation

import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import coil.compose.AsyncImage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.R
import com.example.core.ui.HabitCard
import com.example.core.navigation.Routes
import com.example.core.navigation.navigateToTopLevelDestination
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userPhotoUri by viewModel.userPhotoUri.collectAsStateWithLifecycle()
    val isBannerDismissed by viewModel.isReliableBannerDismissed.collectAsStateWithLifecycle()
    val motivationalQuote by viewModel.motivationalQuote.collectAsStateWithLifecycle()
    // habits and checkedMap are SnapshotStateList/Map — read directly, no collect needed
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow<String?>("habit_save_result", null)?.collect { message ->
            if (message != null) {
                snackbarHostState.showSnackbar(message)
                savedStateHandle.remove<String>("habit_save_result")
            }
        }
    }

    val lazyListState = rememberLazyListState()
    val homeScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is HomeViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    // Check if permissions are fully configured
    var isFullyProtected by remember {
        mutableStateOf(true)
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                // Offload Binder calls (Settings.canDrawOverlays,
                // PowerManager.isIgnoringBatteryOptimizations) to IO to avoid
                // blocking the main thread during the resume callback.
                homeScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
                    val hasOverlay = Settings.canDrawOverlays(context)
                    val hasBatteryExemption = com.example.core.util.BackgroundReliabilityHelper.isIgnoringBatteryOptimizations(context)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        isFullyProtected = hasNotification && hasOverlay && hasBatteryExemption
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val app = context.applicationContext as com.example.app.HabitApplication
    val appLanguage by app.preferencesManager.appLanguageFlow.collectAsStateWithLifecycle(initialValue = "system")

    val hour = LocalTime.now().hour
    val greetingRes = when {
        hour in 5..11 -> com.example.R.string.home_good_morning
        hour in 12..16 -> com.example.R.string.home_good_afternoon
        hour in 17..21 -> com.example.R.string.home_good_evening
        else -> com.example.R.string.home_good_night
    }
    val greeting = stringResource(greetingRes)

    val appLocale = remember(appLanguage) { com.example.core.util.LocaleDirectionHelper.getLocale(appLanguage) }
    val todayDateFormatted = remember(appLocale) {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", appLocale))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        clip = false,
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                    .clickable { navController.navigate(Routes.ADD_HABIT) }
                    .testTag("add_habit_fab"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(com.example.R.string.add_habit),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        val screenBgModifier = if (androidx.compose.foundation.isSystemInDarkTheme())
            Modifier.background(MaterialTheme.colorScheme.background)
        else
            Modifier.background(com.example.core.ui.theme.LightBackgroundGradientBrush)

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .then(screenBgModifier)
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            userScrollEnabled = true,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (isLoading) {
                item {
                    // ===== Loading shimmer placeholder =====
                    val shimmerTransition = rememberInfiniteTransition(label = "LoadingShimmer")
                    val shimmerAnim by shimmerTransition.animateFloat(
                        initialValue = 0f, targetValue = 2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ), label = "shimmer"
                    )
                    val shimmerBrush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(shimmerAnim * 500f, 0f)
                    )
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(54.dp).clip(CircleShape).background(shimmerBrush))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.width(100.dp).height(14.dp).clip(RoundedCornerShape(7.dp)).background(shimmerBrush))
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.width(140.dp).height(20.dp).clip(RoundedCornerShape(10.dp)).background(shimmerBrush))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(shimmerBrush))
                        Spacer(modifier = Modifier.height(32.dp))
                        Box(modifier = Modifier.width(120.dp).height(18.dp).clip(RoundedCornerShape(9.dp)).background(shimmerBrush))
                        Spacer(modifier = Modifier.height(16.dp))
                        repeat(3) {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(20.dp)).background(shimmerBrush))
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            } else {
                // ---- Redesigned Unified Header ----
                item {
                    com.example.core.ui.GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Top Row: [Icons] ... [Greeting + Avatar]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Side 1: Circular glass icon buttons
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    HeaderIconButton(
                                        icon = Icons.Default.Notifications,
                                        onClick = { navController.navigate(Routes.NOTIFICATIONS) },
                                        testTag = "notification_bell"
                                    )
                                    HeaderIconButton(
                                        icon = Icons.Default.CalendarToday,
                                        onClick = { navController.navigate(Routes.CALENDAR) },
                                        testTag = "calendar_button"
                                    )
                                }

                                // Side 2: Greeting + Avatar
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = greeting,
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = userName,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    // Avatar
                                    if (userPhotoUri.isNotEmpty()) {
                                        AsyncImage(
                                            model = coil.request.ImageRequest.Builder(LocalContext.current)
                                                .data(userPhotoUri)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Profile Photo",
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    } else {
                                        val initial = userName.trim().take(1).uppercase().ifEmpty { "U" }
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF7C4DFF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = initial,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            // Middle: Subtitle
                            Text(
                                text = motivationalQuote,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontStyle = FontStyle.Italic,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Bottom: Date Pill
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(
                                        width = 0.5.dp,
                                        color = Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = Color(0xFFB39DDB), // Light lavender
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = todayDateFormatted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }
                }

                // ---- Reliable Reminders Banner ----
                if (!isFullyProtected && !isBannerDismissed) {
                    item {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            com.example.core.ui.GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reliable_reminders_banner"),
                                habitColor = MaterialTheme.colorScheme.error,
                                onClick = { navController.navigate(Routes.SETTINGS) },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.home_banner_title),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(R.string.home_banner_desc),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.dismissReliableBanner() },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("dismiss_banner_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ---- Active Habits Section Header ----
                item {
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(com.example.R.string.home_active_habits),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            TextButton(
                                onClick = { navController.navigateToTopLevelDestination(Routes.ALL_HABITS) },
                                modifier = Modifier.testTag("see_all_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(com.example.R.string.home_see_all),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ---- Habits or Empty State ----
                if (viewModel.habits.isEmpty()) {
                    item {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            com.example.core.ui.GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(com.example.R.string.home_no_active_habits),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "\"${motivationalQuote}\"",
                                        fontSize = 14.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = { navController.navigate(Routes.ADD_HABIT) },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(text = stringResource(com.example.R.string.add_habit))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(
                        items = viewModel.habits,
                        key = { it.habit.id },
                        contentType = { "HabitCard" }
                    ) { item ->
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            val isChecked = viewModel.checkedMap[item.habit.id] ?: false
                            val onCheckedToggle = remember(item.habit.id, viewModel) {
                                { checked: Boolean -> viewModel.toggleCheckIn(item.habit.id, checked) }
                            }
                            val onClick = remember(item.habit.id, navController) {
                                { navController.navigate(Routes.HABIT_DETAIL.replace("{habitId}", item.habit.id.toString())) }
                            }
                            HabitCard(
                                habit = item.habit,
                                completedDays = item.completedDays,
                                isCheckedToday = isChecked,
                                onCheckedToggle = onCheckedToggle,
                                onClick = onClick,
                                modifier = Modifier.testTag("habit_card_${item.habit.id}"),
                                streakDays = item.streakDays,

                            )
                        }
                    }
                }

                // ---- Bottom spacer ----
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        } // end Scaffold
    }
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .border(0.5.dp, Color.White.copy(alpha = 0.15f), CircleShape)
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFB39DDB), // Light lavender
            modifier = Modifier.size(18.dp)
        )
    }
}
