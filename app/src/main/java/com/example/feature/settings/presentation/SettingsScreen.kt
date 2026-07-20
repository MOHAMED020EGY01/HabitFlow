package com.example.feature.settings.presentation

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.core.navigation.Routes
import com.example.core.audio.AudioEngineType
import com.example.core.audio.TTSStatus
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var showResetDialog by remember { mutableStateOf(false) }
    var editNameMode by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Alarm Sound Picker Launcher
    val alarmPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            uri?.let { viewModel.setAlarmUri(it.toString()) }
        }
    }

    // Register ActivityResult Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updateUserPhoto(it.toString())
        }
    }

    val isArabic = if (uiState.appLanguage == "system") {
        androidx.core.os.ConfigurationCompat.getLocales(context.resources.configuration)
            .get(0)?.language == "ar"
    } else {
        uiState.appLanguage == "ar"
    }
    Scaffold(
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
                .imePadding()
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(com.example.R.string.settings),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Profile Visual Card
            com.example.core.ui.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.userPhotoUri.isNotEmpty()) {
                            AsyncImage(
                                model = coil.request.ImageRequest.Builder(LocalContext.current)
                                    .data(uiState.userPhotoUri)
                                    .crossfade(true)
                                    .size(250, 250)
                                    .build(),
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile Photo Placeholder",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        // Small floating edit icon on the avatar
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit photo",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (editNameMode) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = tempName,
                                onValueChange = { tempName = it },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .width(180.dp)
                                    .testTag("edit_username_input")
                            )
                            Button(
                                onClick = {
                                    if (tempName.trim().length >= 2) {
                                        viewModel.updateUserName(tempName.trim())
                                        editNameMode = false
                                    }
                                },
                                modifier = Modifier.testTag("save_username_button")
                            ) {
                                Text(text = stringResource(com.example.R.string.save))
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.clickable {
                                tempName = uiState.userName
                                editNameMode = true
                            }
                        ) {
                            Text(
                                text = uiState.userName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit name",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Language Selector Card
            com.example.core.ui.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(com.example.R.string.language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    val langOptions = listOf(
                        "system" to (if (isArabic) "النظام" else "System"),
                        "ar" to "العربية",
                        "en" to "English"
                    )

                    com.example.core.ui.GlassDropdown(
                        selectedOption = uiState.appLanguage,
                        options = langOptions,
                        onOptionSelected = { langCode ->
                            if (uiState.appLanguage != langCode) {
                                viewModel.updateLanguage(langCode)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Card Animations Configuration Card
            com.example.core.ui.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(com.example.R.string.card_animations),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(com.example.R.string.card_animations_desc),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isCardAnimationsEnabled,
                        onCheckedChange = { viewModel.setCardAnimationsEnabled(it) },
                        modifier = Modifier.testTag("card_animations_toggle")
                    )
                }
            }

            // Glass Effect Configuration Card
            com.example.core.ui.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(com.example.R.string.glass_effect),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(com.example.R.string.glass_effect_desc),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            0 to stringResource(com.example.R.string.glass_effect_auto),
                            1 to stringResource(com.example.R.string.glass_effect_on),
                            2 to stringResource(com.example.R.string.glass_effect_off)
                        ).forEach { (mode, label) ->
                            val isSelected = uiState.glassEffectMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.background
                                    )
                                    .clickable { viewModel.setGlassEffectMode(mode) }
                                    .testTag("glass_effect_$mode"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Hide Nav Bar Toggle Card
            com.example.core.ui.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(com.example.R.string.hide_nav_bar),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(com.example.R.string.hide_nav_bar_desc),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isNavBarHidden,
                        onCheckedChange = { viewModel.setNavBarHidden(it) },
                        modifier = Modifier.testTag("hide_nav_bar_toggle")
                    )
                }
            }

            // Reminder Audio Card
            com.example.core.ui.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(com.example.R.string.reminder_audio_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Engine Selection
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReminderAudioOption(
                            title = stringResource(com.example.R.string.reminder_audio_engine_alarm),
                            selected = uiState.audioSettings.selectedEngine == AudioEngineType.ALARM,
                            onClick = { viewModel.setAudioEngine(AudioEngineType.ALARM) }
                        )

                        val isTtsAvailable = uiState.ttsStatus == TTSStatus.AVAILABLE
                        ReminderAudioOption(
                            title = stringResource(com.example.R.string.reminder_audio_engine_tts),
                            selected = uiState.audioSettings.selectedEngine == AudioEngineType.TTS,
                            enabled = true,
                            onClick = {
                                if (uiState.ttsStatus == TTSStatus.NOT_INSTALLED) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.tts"))
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.tts"))
                                        context.startActivity(webIntent)
                                    }
                                } else {
                                    viewModel.setAudioEngine(AudioEngineType.TTS)
                                }
                            },
                            subtitle = if (!isTtsAvailable && uiState.ttsStatus == TTSStatus.NOT_INSTALLED) 
                                stringResource(com.example.R.string.reminder_audio_tts_not_installed) else null
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    // Contextual Settings
                    if (uiState.audioSettings.selectedEngine == AudioEngineType.ALARM) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Alarm Settings
                            Button(
                                onClick = {
                                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(com.example.R.string.reminder_audio_alarm_select))
                                        val alarmUri = if (uiState.audioSettings.alarmUri.isNotEmpty()) Uri.parse(uiState.audioSettings.alarmUri) else null
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, alarmUri)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                    }
                                    alarmPickerLauncher.launch(intent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = stringResource(com.example.R.string.reminder_audio_alarm_select))
                            }

                            // Duration Selection
                            Column {
                                Text(
                                    text = stringResource(com.example.R.string.reminder_audio_duration),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(
                                        15 to stringResource(com.example.R.string.reminder_audio_duration_15s),
                                        30 to stringResource(com.example.R.string.reminder_audio_duration_30s),
                                        60 to stringResource(com.example.R.string.reminder_audio_duration_60s),
                                        -1 to stringResource(com.example.R.string.reminder_audio_duration_infinite)
                                    ).forEach { (dur, label) ->
                                        val isSel = uiState.audioSettings.alarmDurationSeconds == dur
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { viewModel.setAlarmDuration(dur) }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label.split(" ")[0],
                                                fontSize = 10.sp,
                                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Ringtone Volume Slider
                            AudioSettingSlider(
                                label = stringResource(com.example.R.string.ringtone_volume),
                                currentValue = uiState.audioSettings.ringtoneVolume,
                                onValueChange = { viewModel.setRingtoneVolume(it) },
                                valueRange = 0.0f..1.0f,
                                steps = 99,
                                valueDisplayFormatter = { "${(it * 100).toInt()}%" },
                                presets = listOf(
                                    "Low" to 0.25f,
                                    "Default" to 0.75f,
                                    "High" to 1.0f
                                )
                            )
                        }
                    } else {
                        // TTS Settings
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Repeats
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(com.example.R.string.reminder_audio_tts_repeats), fontSize = 14.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(1, 2).forEach { count ->
                                        val isSel = uiState.audioSettings.ttsRepeats == count
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { viewModel.setTtsRepeats(count) },
                                            label = { Text(if (count == 1) "1x" else "2x") }
                                        )
                                    }
                                }
                            }

                            // Pitch/Rate Sliders (Restore)
                            AudioSettingSlider(
                                label = stringResource(com.example.R.string.speech_pitch),
                                currentValue = uiState.audioSettings.pitch,
                                onValueChange = { viewModel.setSpeechPitch(it) },
                                valueRange = 0.5f..2.0f,
                                steps = 29,
                                valueDisplayFormatter = { String.format(Locale.US, "%.2fx", it) },
                                presets = listOf(
                                    "Low" to 0.5f,
                                    "Default" to 1.0f,
                                    "High" to 1.5f
                                )
                            )

                            AudioSettingSlider(
                                label = stringResource(com.example.R.string.speech_rate),
                                currentValue = uiState.audioSettings.rate,
                                onValueChange = { viewModel.setSpeechRate(it) },
                                valueRange = 0.5f..2.0f,
                                steps = 29,
                                valueDisplayFormatter = { String.format(Locale.US, "%.2fx", it) },
                                presets = listOf(
                                    "Low" to 0.5f,
                                    "Default" to 1.0f,
                                    "High" to 1.5f
                                )
                            )

                            // Voice Volume Slider
                            AudioSettingSlider(
                                label = stringResource(com.example.R.string.voice_volume),
                                currentValue = uiState.audioSettings.voiceVolume,
                                onValueChange = { viewModel.setVoiceVolume(it) },
                                valueRange = 0.0f..1.0f,
                                steps = 99,
                                valueDisplayFormatter = { "${(it * 100).toInt()}%" },
                                presets = listOf(
                                    "Low" to 0.25f,
                                    "Default" to 0.75f,
                                    "High" to 1.0f
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dynamic Test/Stop Button
                    Button(
                        onClick = { viewModel.testReminder() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (uiState.isAudioPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                if (uiState.isAudioPlaying) com.example.R.string.reminder_audio_test_stop
                                else com.example.R.string.reminder_audio_test_button
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Permissions Section
            com.example.core.ui.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(com.example.R.string.permissions),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    NotificationsPermissionRow(isArabic = isArabic)

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    ExactAlarmsPermissionRow(isArabic = isArabic)

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    ReliableRemindersSection(viewModel = viewModel, isArabic = isArabic)
                }
            }

            // Wipe / Reset Card
            com.example.core.ui.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(com.example.R.string.reset_data),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp
                        )
                        Text(
                            text = stringResource(com.example.R.string.reset_data_desc),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showResetDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.testTag("reset_data_button")
                    ) {
                        Text(text = stringResource(com.example.R.string.reset))
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = stringResource(com.example.R.string.reset_data)) },
            text = { Text(text = stringResource(com.example.R.string.confirm_reset)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllData {
                            navController.navigate(Routes.SPLASH) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_reset")
                ) {
                    Text(text = stringResource(com.example.R.string.reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(text = stringResource(com.example.R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun AudioSettingSlider(
    label: String,
    currentValue: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueDisplayFormatter: (Float) -> String,
    presets: List<Pair<String, Float>>
) {
    val scope = rememberCoroutineScope()
    
    // LAYER 1: Local UI State (Source of truth for Slider and Label)
    var localValue by remember { mutableFloatStateOf(currentValue) }
    
    // Helper to ensure callbacks always have the latest value
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    
    // Animation state tracking (not used for dragging, only for presets)
    var isAnimating by remember { mutableStateOf(false) }

    // LAYER 3 Sync: Update local state from Persistent state only when idle
    LaunchedEffect(currentValue) {
        if (!isDragged && !isAnimating) {
            localValue = currentValue
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            // Numeric Display updates immediately from Layer 1
            Text(
                text = valueDisplayFormatter(localValue),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Preset Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { (name, presetValue) ->
                val isSelected = abs(localValue - presetValue) < 0.02f
                val backgroundColor by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    label = "chip_bg"
                )
                val contentColor by animateColorAsState(
                    if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "chip_content"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(backgroundColor)
                        .clickable(enabled = !isAnimating) {
                            // LAYER 2: Animation State
                            scope.launch {
                                isAnimating = true
                                val anim = Animatable(localValue)
                                anim.animateTo(
                                    targetValue = presetValue,
                                    animationSpec = tween(200)
                                ) {
                                    // Update ONLY local UI state during animation frames
                                    localValue = this.value
                                }
                                // Commit to LAYER 3 exactly once after animation finishes
                                currentOnValueChange(presetValue)
                                isAnimating = false
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = contentColor
                    )
                }
            }
        }

        Slider(
            value = localValue,
            onValueChange = {
                // Dragging updates ONLY local UI state (Layer 1)
                localValue = it
            },
            onValueChangeFinished = {
                // Commit to LAYER 3 exactly once after dragging ends
                currentOnValueChange(localValue)
            },
            valueRange = valueRange,
            steps = steps,
            interactionSource = interactionSource,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
fun ReminderAudioOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(enabled = enabled) { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
            }
        }
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
    }
}

@Composable
fun NotificationsPermissionRow(isArabic: Boolean) {
    val context = LocalContext.current
    var hasPermission by remember {
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

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = stringResource(com.example.R.string.notifications),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = if (hasPermission) {
                        if (isArabic) "مسموح ✓" else "Allowed ✓"
                    } else {
                        if (isArabic) "غير مسموح" else "Not allowed"
                    },
                    fontSize = 11.sp,
                    color = if (hasPermission) Color(0xFF69F0AE) else Color(0xFFFF5252)
                )
            }
        }

        if (!hasPermission) {
            TextButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                modifier = Modifier.testTag("grant_notifications_button")
            ) {
                Text(stringResource(com.example.R.string.grant), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ExactAlarmsPermissionRow(isArabic: Boolean) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                am.canScheduleExactAlarms()
            } else {
                true
            }
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                    hasPermission = am.canScheduleExactAlarms()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = stringResource(com.example.R.string.exact_alarms),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = if (hasPermission) {
                        if (isArabic) "مسموح ✓" else "Allowed ✓"
                    } else {
                        if (isArabic) "غير مسموح" else "Not allowed"
                    },
                    fontSize = 11.sp,
                    color = if (hasPermission) Color(0xFF69F0AE) else Color(0xFFFF5252)
                )
            }
        }

        if (!hasPermission) {
            TextButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val intent = Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:${context.packageName}")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.testTag("grant_exact_alarms_button")
            ) {
                Text(stringResource(com.example.R.string.grant), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
