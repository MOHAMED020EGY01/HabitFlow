package com.example.feature.habit.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.core.ui.ColorPicker
import com.example.core.ui.GlassTextField
import com.example.core.ui.TimePickerDialog
import com.example.core.model.domain.ActivationResult
import com.example.core.model.domain.MAX_ACTIVE_HABITS
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddHabitScreen(
    navController: NavController,
    habitId: Int = 0,
    viewModel: AddHabitViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Live-updating habit colour (parsed from the hex string) for the
    // animated glass border on text fields — follows swatch selection.
    val defaultPrimary = MaterialTheme.colorScheme.primary
    val habitColor = remember(uiState.colorHex, defaultPrimary) {
        try {
            Color(android.graphics.Color.parseColor(uiState.colorHex))
        } catch (_: Exception) {
            defaultPrimary
        }
    }

    val presets = listOf(7, 21, 30, 90)
    var isCustomMode by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.id, uiState.durationDays) {
        if (uiState.isEditMode) {
            isCustomMode = uiState.durationDays !in presets
        }
    }

    var showTimePicker by remember { mutableStateOf(false) }
    var pickedHour by remember { mutableIntStateOf(9) }
    var pickedMinute by remember { mutableIntStateOf(0) }

    val activeHabitsCount by viewModel.activeHabitsCount.collectAsStateWithLifecycle()
    val isLimitReached = activeHabitsCount >= MAX_ACTIVE_HABITS && !uiState.wasActiveBefore
    var showLimitDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(habitId) {
        if (habitId > 0) {
            viewModel.loadHabit(habitId)
        }
    }

    val context = LocalContext.current
    val currentLang = remember(context) { 
        context.resources.configuration.locales.get(0).language 
    }

    LaunchedEffect(Unit) {
        viewModel.saveResult.collect { result ->
            val message: String = when (result) {
                is ActivationResult.Activated -> {
                    if (uiState.isEditMode) context.getString(com.example.R.string.habit_updated_activated) else context.getString(com.example.R.string.habit_created_activated)
                }
                is ActivationResult.SavedAsInactive -> {
                    context.getString(com.example.R.string.habit_limit_reached)
                }
                ActivationResult.NotApplicable -> {
                    if (uiState.isEditMode) context.getString(com.example.R.string.habit_updated) else context.getString(com.example.R.string.habit_created)
                }
            }
            navController.previousBackStackEntry?.savedStateHandle?.set("habit_save_result", message)
            navController.popBackStack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) androidx.compose.ui.res.stringResource(com.example.R.string.add_habit_title_edit) else androidx.compose.ui.res.stringResource(com.example.R.string.add_habit_title_create),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
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
                .imePadding()
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Habit Name Input
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(com.example.R.string.add_habit_name_label),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                GlassTextField(
                    value = uiState.name,
                    onValueChange = { viewModel.onNameChange(it) },
                    habitColor = habitColor,
                    placeholder = {
                        Text(
                            text = stringResource(com.example.R.string.add_habit_name_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true,
                    isError = uiState.nameError != null,
                    testTag = "habit_name_input"
                )
                if (uiState.nameError != null) {
                    com.example.core.ui.InlineErrorBanner(
                        message = uiState.nameError!!,
                        modifier = Modifier.padding(top = 4.dp) // tighter padding
                    )
                }
            }

            // Habit Description Input
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(com.example.R.string.add_habit_desc_label),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                GlassTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.onDescriptionChange(it) },
                    habitColor = habitColor,
                    placeholder = {
                        Text(
                            text = stringResource(com.example.R.string.add_habit_desc_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    testTag = "habit_desc_input"
                )
            }

            // Habit Duration Input
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(com.example.R.string.add_habit_duration_label),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Duration Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    presets.forEach { preset ->
                        val isSelected = !isCustomMode && uiState.durationDays == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable {
                                    isCustomMode = false
                                    viewModel.onDurationChange(preset)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = com.example.core.util.AppFormatters.forceWesternDigits(
                                    stringResource(com.example.R.string.add_habit_duration_preset, preset)
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    // Custom Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isCustomMode) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .border(
                                width = 1.dp,
                                color = if (isCustomMode) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(50)
                            )
                            .clickable {
                                isCustomMode = true
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(com.example.R.string.add_habit_duration_custom),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCustomMode) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCustomMode) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // Custom duration input field (only visible when in Custom mode)
                if (isCustomMode) {
                    Spacer(modifier = Modifier.height(12.dp))
                    GlassTextField(
                        value = if (uiState.durationDays == 0) "" else uiState.durationDays.toString(),
                        onValueChange = {
                            val days = it.toIntOrNull() ?: 0
                            viewModel.onDurationChange(days)
                        },
                        habitColor = habitColor,
                        placeholder = { Text(stringResource(com.example.R.string.add_habit_duration_custom_placeholder)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = uiState.durationError != null,
                        testTag = "habit_duration_input"
                    )
                    if (uiState.durationError != null) {
                        com.example.core.ui.InlineErrorBanner(
                            message = uiState.durationError!!,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Days of Week Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                com.example.core.ui.DaysOfWeekSelector(
                    selectedDays = uiState.activeDays,
                    onDaysChanged = { viewModel.onActiveDaysChange(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Color Picker
            ColorPicker(
                selectedColorHex = uiState.colorHex,
                onColorSelected = { viewModel.onColorChange(it) },
                modifier = Modifier.fillMaxWidth()
            )

            // Start Habit Now (Active Status) Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(com.example.R.string.add_habit_start_now),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isLimitReached) {
                        com.example.core.ui.InlineErrorBanner(
                            message = com.example.core.util.AppFormatters.forceWesternDigits(
                                stringResource(com.example.R.string.add_habit_limit_warning, com.example.core.model.domain.MAX_ACTIVE_HABITS)
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        Text(
                            text = com.example.core.util.AppFormatters.forceWesternDigits(
                                stringResource(com.example.R.string.add_habit_activate_desc, activeHabitsCount, com.example.core.model.domain.MAX_ACTIVE_HABITS)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
                Switch(
                    checked = uiState.isActive,
                    onCheckedChange = { checked ->
                        if (checked && isLimitReached) {
                            showLimitDialog = true
                        } else {
                            viewModel.onActiveChange(checked)
                        }
                    },
                    modifier = Modifier.testTag("active_status_switch")
                )
            }

            if (showLimitDialog) {
                AlertDialog(
                    onDismissRequest = { showLimitDialog = false },
                    title = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.add_habit_limit_reached_title)) },
                    text = {
                        Text(androidx.compose.ui.res.stringResource(com.example.R.string.add_habit_limit_reached_desc))
                    },
                    confirmButton = {
                        TextButton(onClick = { showLimitDialog = false }) { Text(androidx.compose.ui.res.stringResource(com.example.R.string.ok)) }
                    }
                )
            }

            // Reminders Section (Card-based style)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(com.example.R.string.reminder_times),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                com.example.core.ui.GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Display existing reminders as chips
                        uiState.reminderTimes.forEach { time ->
                            val parts = time.split(":")
                            val h = parts[0].toInt()
                            val m = parts[1].toInt()
                            val formattedTime = com.example.core.util.AppFormatters.formatTime(h, m, langCode = currentLang)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primary) // Brand color accent
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = formattedTime,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { viewModel.removeReminderTime(time) }
                                    )
                                }
                            }
                        }

                        // Add Reminder Chip Button (Dashed/Outlined style)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { showTimePicker = true }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("add_reminder_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(com.example.R.string.add_habit_add_reminder),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (uiState.reminderTimesError != null) {
                    com.example.core.ui.InlineErrorBanner(
                        message = uiState.reminderTimesError!!,
                        modifier = Modifier.padding(top = 4.dp).testTag("reminder_times_error")
                    )
                }

                if (uiState.errorMessage != null) {
                    com.example.core.ui.InlineErrorBanner(
                        message = uiState.errorMessage!!,
                        modifier = Modifier.padding(top = 4.dp).testTag("time_conflict_error")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Save / Confirm Gradient Button
            val isButtonEnabled = uiState.name.isNotBlank() && uiState.durationDays > 0 && uiState.reminderTimes.isNotEmpty()
            val buttonBgModifier = if (isButtonEnabled) {
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primary)
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            }

            Button(
                onClick = { viewModel.saveHabit() },
                enabled = isButtonEnabled,
                shape = RoundedCornerShape(28.dp),
                modifier = buttonBgModifier.testTag("save_habit_button"),
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
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isButtonEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isEditMode) stringResource(com.example.R.string.add_habit_save_changes) else stringResource(com.example.R.string.add_habit_create_habit),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isButtonEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showTimePicker) {
        com.example.core.ui.TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val formattedTime = com.example.core.util.AppFormatters.forceWesternDigits(
                            String.format(
                                Locale.US,
                                "%02d:%02d",
                                pickedHour,
                                pickedMinute
                            )
                        )
                        viewModel.addReminderTime(formattedTime)
                        showTimePicker = false
                    }
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.example.R.string.ok),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.example.R.string.cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        ) {
            com.example.core.ui.WheelTimePicker(
                initialHour = pickedHour,
                initialMinute = pickedMinute,
                langCode = currentLang,
                onTimeChanged = { h, m ->
                    pickedHour = h
                    pickedMinute = m
                }
            )
        }
    }
}
