package com.example.feature.calendar.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.R
import com.example.core.model.domain.Habit
import com.example.core.ui.GlassCard
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavController,
    viewModel: CalendarViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val app = context.applicationContext as com.example.app.HabitApplication
    val appLanguage by app.preferencesManager.appLanguageFlow.collectAsStateWithLifecycle(initialValue = app.currentLanguageCode)
    val appLocale = remember(appLanguage) { com.example.core.util.LocaleDirectionHelper.getLocale(appLanguage) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.calendar_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        val screenBgModifier = if (androidx.compose.foundation.isSystemInDarkTheme())
            Modifier.background(MaterialTheme.colorScheme.background)
        else
            Modifier.background(com.example.core.ui.theme.LightBackgroundGradientBrush)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(screenBgModifier)
                .padding(innerPadding)
        ) {
            // Header: Prev/Next + Range + Mode Toggle
            CalendarHeader(
                selectedDate = uiState.selectedDate,
                mode = uiState.mode,
                onPrev = { viewModel.navigatePrevious() },
                onNext = { viewModel.navigateNext() },
                onModeToggle = { viewModel.toggleMode() },
                locale = appLocale,
                appLanguage = appLanguage
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Crossfade(targetState = uiState.mode, label = "CalendarMode") { mode ->
                    when (mode) {
                        CalendarMode.DAY -> DayView(
                            date = uiState.selectedDate,
                            habits = uiState.habits,
                            locale = appLocale
                        )
                        CalendarMode.WEEK -> WeekView(
                            selectedDate = uiState.selectedDate,
                            habits = uiState.habits,
                            onDateSelect = { viewModel.onDateChange(it) },
                            locale = appLocale
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarHeader(
    selectedDate: LocalDate,
    mode: CalendarMode,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onModeToggle: () -> Unit,
    locale: Locale,
    appLanguage: String
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Date Range Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrev) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous"
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val rangeText = remember(selectedDate, mode, locale) {
                    if (mode == CalendarMode.DAY) {
                        // الأحد، ١٢ يوليو ٢٠٢٦ -> force Western digits
                        com.example.core.util.AppFormatters.formatDate(selectedDate, "EEEE, d MMMM yyyy", langCode = appLanguage)
                    } else {
                        val firstDay = com.example.core.util.AppFormatters.getStartOfWeek(selectedDate)
                        val lastDay = com.example.core.util.AppFormatters.getEndOfWeek(selectedDate)
                        
                        if (firstDay.year == lastDay.year) {
                            if (firstDay.month == lastDay.month) {
                                // 6 - 12 يوليو ٢٠٢٦
                                val d1 = com.example.core.util.AppFormatters.forceWesternDigits(firstDay.dayOfMonth.toString())
                                val d2 = com.example.core.util.AppFormatters.forceWesternDigits(lastDay.dayOfMonth.toString())
                                val monthYear = com.example.core.util.AppFormatters.formatDate(firstDay, "MMMM yyyy", langCode = appLanguage)
                                "$d1 - $d2 $monthYear"
                            } else {
                                // ٢٩ يونيو - ٥ يوليو ٢٠٢٦
                                val d1m = com.example.core.util.AppFormatters.formatDate(firstDay, "d MMMM", langCode = appLanguage)
                                val d2my = com.example.core.util.AppFormatters.formatDate(lastDay, "d MMMM yyyy", langCode = appLanguage)
                                "$d1m - $d2my"
                            }
                        } else {
                            // ٢٩ ديسمبر ٢٠٢٥ - ٤ يناير ٢٠٢٦
                            val d1my = com.example.core.util.AppFormatters.formatDate(firstDay, "d MMMM yyyy", langCode = appLanguage)
                            val d2my = com.example.core.util.AppFormatters.formatDate(lastDay, "d MMMM yyyy", langCode = appLanguage)
                            "$d1my - $d2my"
                        }
                    }
                }

                Text(
                    text = rangeText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                val secondaryLabel = remember(selectedDate, mode) {
                    val today = LocalDate.now()
                    if (mode == CalendarMode.DAY) {
                        if (selectedDate == today) context.getString(com.example.R.string.today) else ""
                    } else {
                        val firstDay = com.example.core.util.AppFormatters.getStartOfWeek(selectedDate)
                        val lastDay = com.example.core.util.AppFormatters.getEndOfWeek(selectedDate)
                        if (!today.isBefore(firstDay) && !today.isAfter(lastDay)) context.getString(com.example.R.string.calendar_this_week) else ""
                    }
                }

                if (secondaryLabel.isNotEmpty()) {
                    Text(
                        text = secondaryLabel,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Segmented Toggle
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                val daySelected = mode == CalendarMode.DAY
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (daySelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { if (!daySelected) onModeToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.calendar_day),
                        color = if (daySelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (!daySelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { if (daySelected) onModeToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.calendar_week),
                        color = if (!daySelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DayView(date: LocalDate, habits: List<Habit>, locale: Locale) {
    val scheduledEntries = remember(date, habits) {
        habits.filter { it.isActive && it.isScheduledOn(date) }
            .flatMap { habit ->
                habit.reminderTimes.map { timeStr ->
                    val time = try {
                        LocalTime.parse(timeStr)
                    } catch (_: Exception) {
                        LocalTime.MIDNIGHT
                    }
                    CalendarEntry(habit, time)
                }
            }
            .sortedBy { it.time }
    }

    if (scheduledEntries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.calendar_no_habits_today),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(scheduledEntries) { entry ->
                TimelineRow(entry, locale)
            }
        }
    }
}

@Composable
fun TimelineRow(entry: CalendarEntry, locale: Locale) {
    val habitColor = remember(entry.habit.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(entry.habit.colorHex))
        } catch (_: Exception) {
            Color(0xFF7C4DFF)
        }
    }

    val isArabic = locale.language == "ar"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Time Label
        Text(
            text = com.example.core.util.AppFormatters.formatTime(entry.time, isArabic),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.width(80.dp) // increased width for full Arabic words
        )

        // Vertical Line and Dot
        Column(
            modifier = Modifier
                .width(24.dp)
                .height(IntrinsicSize.Min),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(habitColor)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            )
        }

        // Habit Card
        GlassCard(
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
            habitColor = habitColor,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Leading Accent Border
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(3.dp)
                        .background(habitColor)
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(habitColor.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = entry.habit.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        text = formatTimeLocalized(entry.time, locale),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WeekView(
    selectedDate: LocalDate,
    habits: List<Habit>,
    onDateSelect: (LocalDate) -> Unit,
    locale: Locale
) {
    val weekStart = com.example.core.util.AppFormatters.getStartOfWeek(selectedDate)
    val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 7-day grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            weekDays.forEach { date ->
                val isSelected = date == selectedDate
                val scheduledHabitsForThisDay = habits.filter { it.isActive && it.isScheduledOn(date) }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { onDateSelect(date) }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = com.example.core.util.AppFormatters.getFullDayName(date.dayOfWeek, locale),
                        fontSize = 9.sp, // slightly smaller to fit full names
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = com.example.core.util.AppFormatters.forceWesternDigits(date.dayOfMonth.toString()),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Dots
                    val maxDots = 4
                    Row(
                        modifier = Modifier.height(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (scheduledHabitsForThisDay.size <= maxDots) {
                            scheduledHabitsForThisDay.forEach { habit ->
                                val color = remember(habit.colorHex) {
                                    try { Color(android.graphics.Color.parseColor(habit.colorHex)) } catch (_: Exception) { Color.Gray }
                                }
                                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(color))
                            }
                        } else {
                            repeat(maxDots - 1) { i ->
                                val color = remember(scheduledHabitsForThisDay[i].colorHex) {
                                    try { Color(android.graphics.Color.parseColor(scheduledHabitsForThisDay[i].colorHex)) } catch (_: Exception) { Color.Gray }
                                }
                                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(color))
                            }
                            Text(
                                text = "+${scheduledHabitsForThisDay.size - (maxDots - 1)}",
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Selected Day Details
        val selectedDayEntries = remember(selectedDate, habits) {
            habits.filter { it.isActive && it.isScheduledOn(selectedDate) }
                .flatMap { habit ->
                    habit.reminderTimes.map { timeStr ->
                        val time = try { LocalTime.parse(timeStr) } catch (_: Exception) { LocalTime.MIDNIGHT }
                        CalendarEntry(habit, time)
                    }
                }
                .sortedBy { it.time }
        }

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            val isArabic = locale.language == "ar"
            Column(modifier = Modifier.padding(20.dp)) {
                val headerText = remember(selectedDate, locale) {
                    com.example.core.util.AppFormatters.formatDate(selectedDate, "EEEE, d MMMM yyyy", locale)
                }
                Text(
                    text = headerText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (selectedDayEntries.isEmpty()) {
                    Text(
                        text = stringResource(R.string.calendar_no_habits),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                } else {
                    selectedDayEntries.forEach { entry ->
                        val habitColor = remember(entry.habit.colorHex) {
                            try { Color(android.graphics.Color.parseColor(entry.habit.colorHex)) } catch (_: Exception) { Color.Gray }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(habitColor))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${entry.habit.name} — ${com.example.core.util.AppFormatters.formatTime(entry.time, isArabic)}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

data class CalendarEntry(
    val habit: Habit,
    val time: LocalTime
)

fun formatTimeLocalized(time: LocalTime, locale: Locale): String {
    return com.example.core.util.AppFormatters.formatTime(time, locale.language == "ar")
}

