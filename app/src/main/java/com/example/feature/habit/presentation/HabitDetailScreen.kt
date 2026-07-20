package com.example.feature.habit.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.time.format.TextStyle
import android.widget.Toast
import com.example.core.ui.ProgressRing
import com.example.core.navigation.Routes
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.DayOfWeek
import java.util.Locale
import com.example.core.util.NextReminderCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    navController: NavController,
    habitId: Int,
    viewModel: HabitDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var showDeleteDialog by remember { mutableStateOf(false) }
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

    LaunchedEffect(habitId) {
        viewModel.observeHabitDetails(habitId)
    }

    val habitColor = remember(uiState.habitDetails) {
        try {
            Color(android.graphics.Color.parseColor(uiState.habitDetails?.habit?.colorHex ?: "#7C4DFF"))
        } catch (e: Exception) {
            Color(0xFF7C4DFF)
        }
    }

    val androidContext = LocalContext.current
    val isArabic = remember {
        val app = androidContext.applicationContext as? com.example.app.HabitApplication
        app?.currentLanguageCode == "ar" || 
        (app?.currentLanguageCode == "system" && androidx.core.os.ConfigurationCompat.getLocales(androidContext.resources.configuration).get(0)?.language == "ar")
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { innerPadding ->
        val detailBgModifier = if (androidx.compose.foundation.isSystemInDarkTheme())
            Modifier.background(MaterialTheme.colorScheme.background)
        else
            Modifier.background(com.example.core.ui.theme.LightBackgroundGradientBrush)

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(detailBgModifier),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.habitDetails == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(detailBgModifier),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(com.example.R.string.habit_not_found), fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            }
        } else {
            val details = uiState.habitDetails!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(detailBgModifier)
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Hero Card Box (for overlapping effect)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        com.example.core.ui.GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            habitColor = habitColor,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                habitColor.copy(alpha = 0.35f),
                                                habitColor.copy(alpha = 0.15f)
                                            )
                                        )
                                    )
                                    .padding(bottom = 44.dp) // extra padding to allow overlapping chips
                             ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    // Top Row inside Hero gradient: Back, Title, Edit, Delete
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { navController.popBackStack() },
                                            modifier = Modifier
                                                .background(Color.White.copy(alpha = 0.2f), shape = CircleShape)
                                                .testTag("back_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back",
                                                tint = Color.White
                                            )
                                        }

                                        Text(
                                            text = androidx.compose.ui.res.stringResource(com.example.R.string.habit_details_title),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            IconButton(
                                                onClick = {
                                                    navController.navigate(Routes.ADD_HABIT.replace("{habitId}", habitId.toString()))
                                                },
                                                modifier = Modifier
                                                    .background(Color.White.copy(alpha = 0.2f), shape = CircleShape)
                                                    .testTag("edit_habit_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Habit",
                                                    tint = Color.White
                                                )
                                            }

                                            IconButton(
                                                onClick = { showDeleteDialog = true },
                                                modifier = Modifier
                                                    .background(Color.White.copy(alpha = 0.2f), shape = CircleShape)
                                                    .testTag("delete_habit_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Habit",
                                                    tint = Color.White
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Text(
                                        text = details.habit.name,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    if (details.habit.description.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = details.habit.description,
                                            fontSize = 14.sp,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(44.dp)) // height for overlapping chips
                    }

                    // Overlapping Stat Chips Row
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatChip(
                            icon = Icons.Default.CheckCircle,
                            iconColor = Color(0xFF00E676),
                            label = androidx.compose.ui.res.stringResource(com.example.R.string.habit_detail_done),
                            value = com.example.core.util.AppFormatters.forceWesternDigits(
                                androidContext.getString(com.example.R.string.habit_detail_days, details.completedDays)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            icon = Icons.Default.Close,
                            iconColor = Color(0xFFFF1744),
                            label = androidx.compose.ui.res.stringResource(com.example.R.string.habit_detail_missed),
                            value = com.example.core.util.AppFormatters.forceWesternDigits(
                                androidContext.getString(com.example.R.string.habit_detail_days, uiState.missedDaysCount)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            icon = Icons.Default.Star,
                            iconColor = Color(0xFF00E5FF),
                            label = androidx.compose.ui.res.stringResource(com.example.R.string.habit_detail_goal),
                            value = com.example.core.util.AppFormatters.forceWesternDigits(
                                androidContext.getString(com.example.R.string.habit_detail_total, details.habit.getScheduledDaysCount())
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 2. Progress Card
                com.example.core.ui.GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    habitColor = habitColor,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val scheduledTotal = details.habit.getScheduledDaysCount()
                        val progress = if (scheduledTotal > 0) {
                            details.completedDays.toFloat() / scheduledTotal
                        } else 0f

                        val lighterColor = remember(habitColor) {
                            try {
                                Color(
                                    red = (habitColor.red + 0.15f).coerceAtMost(1f),
                                    green = (habitColor.green + 0.15f).coerceAtMost(1f),
                                    blue = (habitColor.blue + 0.15f).coerceAtMost(1f),
                                    alpha = habitColor.alpha
                                )
                            } catch (e: Exception) {
                                habitColor
                            }
                        }

                        ProgressRing(
                            progress = progress,
                            gradientColors = listOf(
                                habitColor,
                                lighterColor
                            ),
                            size = 140.dp,
                            strokeWidth = 12.dp,
                            textColor = MaterialTheme.colorScheme.onSurface,
                            textSize = 32.sp,
                            backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = androidx.compose.ui.res.stringResource(com.example.R.string.habit_detail_overall_progress),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = com.example.core.util.AppFormatters.forceWesternDigits(
                                if (uiState.daysRemaining > 0) {
                                    androidContext.getString(com.example.R.string.habit_detail_days_remaining, uiState.daysRemaining)
                                } else {
                                    androidContext.getString(com.example.R.string.habit_detail_goal_reached)
                                }
                            ),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        val cycleLogs = details.logs
                        val doneCount = cycleLogs.count { it.state == "DONE" || it.completed }
                        val missCount = uiState.missedDaysCount
                        val inactiveCount = cycleLogs.count { it.state == "INACTIVE_SKIPPED" }
                        // For the progress bar segments, we want future scheduled days only.
                        // totalScheduled = done + miss + futureScheduled
                        val totalScheduled = details.habit.getScheduledDaysCount()
                        val futureRemainingCount = (totalScheduled - doneCount - missCount).coerceAtLeast(0)
                        
                        MultiSegmentProgressBar(
                            doneCount = doneCount,
                            missCount = missCount,
                            inactiveCount = inactiveCount,
                            remainingCount = futureRemainingCount,
                            habitColor = habitColor
                        )
                    }
                }

                // 3. Reminders Card
                RemindersCard(
                    reminderTimes = details.habit.reminderTimes,
                    habitColor = habitColor,
                    activeDays = details.habit.activeDays
                )

                // 4. "Mark Today as Done" Button
                val isTodayCompleted = remember(details.logs) {
                    details.logs.any { it.logDate == LocalDate.now().toString() && it.completed }
                }
                val isHabitActive = details.habit.status == com.example.core.model.domain.HabitStatus.ACTIVE

                if (isTodayCompleted) {
                    Button(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            .testTag("mark_today_done_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.example.R.string.habit_detail_already_done),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                } else if (!isHabitActive) {
                    Button(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            .testTag("mark_today_done_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = stringResource(com.example.R.string.habit_not_active),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            if (details.habit.isActiveToday()) {
                                viewModel.toggleLogForDate(LocalDate.now().toString(), true)
                            } else {
                                val message = if (isArabic) 
                                    "هذه العادة غير مجدولة لليوم" 
                                else 
                                    "This habit isn't scheduled for today"
                                Toast.makeText(androidContext, message, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        habitColor,
                                        Color(0xFF00E5FF)
                                    )
                                )
                            )
                            .testTag("mark_today_done_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.example.R.string.habit_detail_mark_done),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                }

                // 4.5 Habit Status Actions
                val habitStatus = details.habit.status
                if (habitStatus == com.example.core.model.domain.HabitStatus.ACTIVE) {
                    OutlinedButton(
                        onClick = { viewModel.pauseHabit() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pause_habit_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(
                            text = stringResource(com.example.R.string.habit_pause),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (habitStatus == com.example.core.model.domain.HabitStatus.INACTIVE) {
                    Button(
                        onClick = { viewModel.resumeHabit() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("resume_habit_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(
                            text = stringResource(com.example.R.string.habit_resume),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else if (habitStatus == com.example.core.model.domain.HabitStatus.COMPLETE || habitStatus == com.example.core.model.domain.HabitStatus.FAILURE) {
                    Button(
                        onClick = { viewModel.restartHabit() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("restart_habit_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(
                            text = stringResource(com.example.R.string.habit_restart),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }

                // 5. Consistency Heatmap Card
                ConsistencyHeatmap(
                    habit = details.habit,
                    logs = details.logs,
                    completedColor = habitColor
                )

                // 5.5 Previous Cycles
                PreviousCyclesCard(previousCycles = uiState.previousCycles, habitColor = habitColor)

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showDeleteDialog) {
        com.example.core.ui.GlassDeleteModal(
            title = stringResource(com.example.R.string.delete_habit_dialog_title),
            message = stringResource(com.example.R.string.delete_habit_dialog_text),
            confirmText = stringResource(com.example.R.string.delete),
            cancelText = stringResource(com.example.R.string.cancel),
            onConfirm = {
                viewModel.deleteHabit {
                    navController.popBackStack()
                }
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    com.example.core.ui.GlassCard(
        modifier = modifier,
        habitColor = iconColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                color = iconColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemindersCard(
    reminderTimes: List<String>,
    habitColor: Color? = null,
    activeDays: Set<DayOfWeek> = DayOfWeek.values().toSet()
) {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    val context = LocalContext.current
    val isArabic = remember {
        val app = context.applicationContext as? com.example.app.HabitApplication
        app?.currentLanguageCode == "ar" || 
        (app?.currentLanguageCode == "system" && androidx.core.os.ConfigurationCompat.getLocales(context.resources.configuration).get(0)?.language == "ar")
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            currentTime = LocalDateTime.now()
        }
    }

    val nextReminder = remember(reminderTimes, currentTime, activeDays) {
        val parsedTimes = reminderTimes.mapNotNull {
            try {
                val parts = it.split(":")
                LocalTime.of(parts[0].toInt(), parts[1].toInt())
            } catch (e: Exception) {
                null
            }
        }
        NextReminderCalculator.getNextReminderDateTime(parsedTimes, currentTime, activeDays)
    }

    val scheduleText = remember(activeDays, isArabic) {
        if (activeDays.size == 7) {
            context.getString(com.example.R.string.daily)
        } else {
            val locale = if (isArabic) Locale("ar") else Locale.ENGLISH
            activeDays.sortedBy { if (it == DayOfWeek.SUNDAY) 0 else it.value } // Sunday first
                .joinToString(", ") { com.example.core.util.AppFormatters.getFullDayName(it, locale) }
        }
    }

    com.example.core.ui.GlassCard(
        modifier = Modifier.fillMaxWidth(),
        habitColor = habitColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.R.string.habit_detail_reminders),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (nextReminder != null) {
                    val timeStr = NextReminderCalculator.formatTime(nextReminder, isArabic)
                    Text(
                        text = "$scheduleText • $timeStr",
                        fontSize = 11.sp,
                        color = (habitColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (reminderTimes.isEmpty()) {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.R.string.habit_detail_no_reminders),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    reminderTimes.forEach { time ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                              ) {
                                  Text(
                                      text = "⏰",
                                      fontSize = 14.sp
                                  )
                                  Text(
                                      text = formatTime12Hour(time),
                                      fontSize = 14.sp,
                                      color = MaterialTheme.colorScheme.onSurface,
                                      fontWeight = FontWeight.Medium
                                  )
                              }
                          }
                      }
                  }
              }
          }
      }
  }

@Composable
fun ConsistencyHeatmap(
    habit: com.example.core.model.domain.Habit,
    logs: List<com.example.core.model.domain.HabitLog>,
    completedColor: Color
) {
    val startDate = java.time.Instant.ofEpochMilli(habit.cycleStartDate)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
    
    val cycleEndDate = java.time.Instant.ofEpochMilli(habit.cycleEndDate)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
    
    val today = LocalDate.now()
    val displayEndDate = if (cycleEndDate.isAfter(today)) cycleEndDate else today

    // Fix Bug 2: Always start from Sunday
    val firstDayInGrid = com.example.core.util.AppFormatters.getStartOfWeek(startDate)
    val lastDayInGrid = com.example.core.util.AppFormatters.getEndOfWeek(displayEndDate)

    val gridDates = remember(firstDayInGrid, lastDayInGrid) {
        val dates = mutableListOf<LocalDate>()
        var curr = firstDayInGrid
        while (!curr.isAfter(lastDayInGrid)) {
            dates.add(curr)
            curr = curr.plusDays(1)
        }
        dates
    }

    com.example.core.ui.GlassCard(
        modifier = Modifier.fillMaxWidth(),
        habitColor = completedColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.example.R.string.habit_detail_consistency_heatmap),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(completedColor)
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.example.R.string.habit_detail_legend_done),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.example.R.string.habit_detail_legend_empty),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            val locale = androidx.compose.ui.platform.LocalContext.current.resources.configuration.locales.get(0)
            val dayHeaders = remember(locale) {
                listOf(
                    DayOfWeek.SUNDAY,
                    DayOfWeek.MONDAY,
                    DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY,
                    DayOfWeek.SATURDAY
                ).map { com.example.core.util.AppFormatters.getFullDayName(it, locale) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayHeaders.forEach { header ->
                    Text(
                        text = header,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            val weeks = gridDates.chunked(7)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                weeks.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        week.forEach { date ->
                            val logForDate = remember(logs, date) {
                                logs.find { it.logDate == date.toString() }
                            }
                            val isCompleted = logForDate?.state == "DONE" || (logForDate?.completed ?: false)
                            val isMissed = logForDate?.state == "MISS"
                            val isInactiveSkipped = logForDate?.state == "INACTIVE_SKIPPED"
                            val isToday = date == today
                            val isFuture = date.isAfter(today)
                            val isScheduled = habit.isScheduledOn(date)

                            val circleColor = when {
                                isCompleted -> completedColor
                                isMissed -> Color(0xFFFFCDD2)
                                isInactiveSkipped -> Color(0xFFFFE082)
                                !isScheduled -> MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                                isFuture -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }

                            val borderStroke = if (isToday) {
                                BorderStroke(2.dp, completedColor)
                            } else null

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("heatmap_day_${date}"),
                                    shape = CircleShape,
                                    colors = CardDefaults.cardColors(
                                        containerColor = circleColor
                                    ),
                                    border = borderStroke
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isInactiveSkipped) "X" else com.example.core.util.AppFormatters.forceWesternDigits(date.dayOfMonth.toString()),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isCompleted -> Color.Black
                                                isMissed -> Color(0xFFC62828)
                                                isInactiveSkipped -> Color(0xFFEF6C00)
                                                !isScheduled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MultiSegmentProgressBar(
    doneCount: Int,
    missCount: Int,
    inactiveCount: Int,
    remainingCount: Int,
    habitColor: Color
) {
    val total = (doneCount + missCount + inactiveCount + remainingCount).toFloat().coerceAtLeast(1f)
    val doneWeight = doneCount / total
    val missWeight = missCount / total
    val inactiveWeight = inactiveCount / total
    val remainingWeight = remainingCount / total

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (doneWeight > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(doneWeight)
                        .background(Color(0xFF00E676))
                )
            }
            if (missWeight > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(missWeight)
                        .background(Color(0xFFFF1744))
                )
            }
            if (inactiveWeight > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(inactiveWeight)
                        .background(Color(0xFFFFB300))
                )
            }
            if (remainingWeight > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(remainingWeight)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(label = "Done", count = doneCount, color = Color(0xFF00E676))
            LegendItem(label = "Missed", count = missCount, color = Color(0xFFFF1744))
            LegendItem(label = "Paused", count = inactiveCount, color = Color(0xFFFFB300))
            LegendItem(label = "Remaining", count = remainingCount, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}

@Composable
fun LegendItem(label: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = com.example.core.util.AppFormatters.forceWesternDigits("$label ($count)"),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PreviousCyclesCard(
    previousCycles: List<com.example.core.model.domain.HabitCycleHistory>,
    habitColor: Color? = null
) {
    if (previousCycles.isEmpty()) return

    com.example.core.ui.GlassCard(
        modifier = Modifier.fillMaxWidth(),
        habitColor = habitColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(com.example.R.string.previous_cycles),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            previousCycles.forEach { cycle ->
                val isSuccess = cycle.result == "COMPLETE"
                val resultColor = if (isSuccess) Color(0xFF00E676) else Color(0xFFFF1744)
                
                val startLocalDate = java.time.Instant.ofEpochMilli(cycle.cycleStartDate)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                val endLocalDate = java.time.Instant.ofEpochMilli(cycle.cycleEndDate)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()

                val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")
                val dateRangeStr = com.example.core.util.AppFormatters.forceWesternDigits(
                    "${startLocalDate.format(formatter)} - ${endLocalDate.format(formatter)}"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = dateRangeStr,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val localizedResult = when(cycle.result) {
                            "COMPLETE" -> stringResource(com.example.R.string.result_complete)
                            "FAILURE" -> stringResource(com.example.R.string.result_failure)
                            else -> cycle.result
                        }
                        Text(
                            text = stringResource(com.example.R.string.cycle_result, localizedResult),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = resultColor
                        )
                    }
                    Text(
                        text = com.example.core.util.AppFormatters.forceWesternDigits("${cycle.completionPercentage.toInt()}%"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = resultColor
                    )
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            }
        }
    }
}

fun formatTime12Hour(timeStr: String): String {
    return try {
        val parts = timeStr.split(":")
        if (parts.size == 2) {
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            
            val app = com.example.app.HabitApplication.instance
            val langCode = app.currentLanguageCode
            
            com.example.core.util.AppFormatters.formatTime(hour, minute, langCode = langCode)
        } else {
            timeStr
        }
    } catch (e: Exception) {
        timeStr
    }
}
