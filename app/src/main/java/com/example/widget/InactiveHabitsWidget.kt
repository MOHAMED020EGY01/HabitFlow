package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider as ColorProviderType
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextAlign
import com.example.HabitApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

class InactiveHabitsWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as HabitApplication
        
        var inactiveHabits: List<HabitWidgetRepository.WidgetHabitData> = emptyList()
        var langCode = "system"
        var hadError = false

        try {
            app.ensureInitialized(timeoutMs = 2000L)
            
            val repository = HabitWidgetRepository(app.repository)
            
            inactiveHabits = repository.getRecentInactiveHabitsForWidgets(limit = 4)

            langCode = try {
                withTimeout(1000L) {
                    app.preferencesManager.appLanguageFlow.first()
                }
            } catch (e: Exception) {
                "system"
            }

        } catch (e: Exception) {
            hadError = true
        }

        val localizedContext = try {
            com.example.util.LocaleDirectionHelper.getLocalizedContext(context, langCode)
        } catch (e: Exception) {
            context
        }

        provideContent {
            InactiveHabitsWidgetContent(
                context = localizedContext,
                inactiveHabits = inactiveHabits,
                hadError = hadError,
                langCode = langCode
            )
        }
    }
}

@Composable
private fun InactiveHabitsWidgetContent(
    context: Context,
    inactiveHabits: List<HabitWidgetRepository.WidgetHabitData>,
    hadError: Boolean,
    langCode: String
) {
    // Styling constants
    val backgroundColor = ColorProvider(day = Color(0xFFF3F7EF), night = Color(0xFF0F0F1A))
    val glassTint = ColorProvider(day = Color(0xFF5E35B1).copy(alpha = 0.05f), night = Color(0xFF7C4DFF).copy(alpha = 0.15f))
    val borderTint = ColorProvider(day = Color.Black.copy(alpha = 0.08f), night = Color.White.copy(alpha = 0.15f))
    val textColor = ColorProvider(day = Color(0xFF1F2E20), night = Color.White)
    val subTextColor = ColorProvider(day = Color(0xFF1F2E20).copy(alpha = 0.6f), night = Color.White.copy(alpha = 0.6f))
    
    // Resolve RTL based on app language
    val isRtl = com.example.util.LocaleDirectionHelper.isRtl(langCode)
    Log.d("InactiveHabitsWidget", "isRtl" + isRtl + " langCode: " + langCode);
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Outer border
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(borderTint)
                .cornerRadius(20.dp),
            contentAlignment = Alignment.Center
        ) {
            // Inner background
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(1.dp)
                    .background(backgroundColor)
                    .cornerRadius(19.dp),
                contentAlignment = Alignment.Center
            ) {
                // Glass effect overlay
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(glassTint)
                        .cornerRadius(19.dp)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (hadError) {
                        Text(
                            text = context.getString(com.example.R.string.widget_failed_load),
                            style = TextStyle(
                                color = ColorProvider(day = Color.Red, night = Color.Red),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        )
                    } else {
                        Column(
                            modifier = GlanceModifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Title in a smaller, oval-like card
                            Box(
                                modifier = GlanceModifier
                                    .padding(bottom = 3.dp) // Space below title for visual breathing room
                                    .background(ColorProvider(day = Color(0xFF7C4DFF).copy(alpha = 0.2f), night = Color(0xFF7C4DFF).copy(alpha = 0.3f)))
                                    .cornerRadius(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = context.getString(com.example.R.string.widget_inactive_habits_title),
                                    style = TextStyle(
                                        color = ColorProvider(day = Color(0xFF7C4DFF), night = Color.White),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = GlanceModifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = GlanceModifier.height(3.dp))

                            // Render 4 habit slots (actual or placeholder)
                            repeat(4) { index ->
                                if (index > 0) {
                                    Spacer(modifier = GlanceModifier.height(4.dp)) // Tight spacing between cards
                                }
                                val habit = inactiveHabits.getOrNull(index)
                                if (habit != null) {
                                    InactiveHabitCard(
                                        context = context,
                                        habit = habit,
                                        textColor = textColor,
                                        subTextColor = subTextColor,
                                        isRtl = isRtl,
                                        modifier = GlanceModifier.defaultWeight()
                                    )
                                } else {
                                    PlaceholderHabitCard(
                                        context = context,
                                        isRtl = isRtl,
                                        modifier = GlanceModifier.defaultWeight()
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

@Composable
private fun InactiveHabitCard(
    context: Context,
    habit: HabitWidgetRepository.WidgetHabitData,
    textColor: ColorProviderType,
    subTextColor: ColorProviderType,
    isRtl: Boolean,
    modifier: GlanceModifier = GlanceModifier
) {
    val habitColor = try {
        Color(android.graphics.Color.parseColor(habit.colorHex))
    } catch (_: Exception) {
        Color(0xFF7C4DFF)
    }

    Box(
        modifier = modifier
            .background(habitColor.copy(alpha = 0.18f))
            .cornerRadius(12.dp)
            .clickable(actionStartActivity(buildHabitDetailOrAddIntent(context, habit.habitId))),
        contentAlignment = if (isRtl) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isRtl) {
                // Color Stripe on Left for LTR (English)
                Box(
                    modifier = GlanceModifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(habitColor)
                        .cornerRadius(2.dp)
                ) {}
            }

            Column(
                modifier = GlanceModifier.defaultWeight().padding(horizontal = 12.dp, vertical = 2.dp), // Tighter vertical padding for slim look
                horizontalAlignment = if (isRtl) Alignment.Start else Alignment.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = habit.name,
                    maxLines = 1,
                    style = TextStyle(
                        color = textColor,
                        fontSize = 12.sp, // Slightly smaller font for compact layout
                        fontWeight = FontWeight.Bold,
                        textAlign = if (isRtl) TextAlign.End else TextAlign.Start
                    )
                )
                
                val stopTimestamp = habit.inactiveSinceTimestamp
                if (stopTimestamp != null) {
                    val dateText = getFormattedStopDate(context, stopTimestamp)
                    val durationText = getDetailedInactiveDuration(context, stopTimestamp)
                    
                    Text(
                        text = dateText,
                        maxLines = 1,
                        style = TextStyle(
                            color = subTextColor,
                            fontSize = 8.sp,
                            textAlign = if (isRtl) TextAlign.End else TextAlign.Start
                        )
                    )
                    Text(
                        text = durationText,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(habitColor.copy(alpha = 0.9f), Color.White.copy(alpha = 0.8f)),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = if (isRtl) TextAlign.End else TextAlign.Start
                        )
                    )
                }
            }

            if (isRtl) {
                // Color Stripe on Right for RTL (Arabic)
                Box(
                    modifier = GlanceModifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(habitColor)
                        .cornerRadius(2.dp)
                ) {}
            }
        }
    }
}

@Composable
private fun PlaceholderHabitCard(
    context: Context,
    isRtl: Boolean,
    modifier: GlanceModifier = GlanceModifier
) {
    val primaryColor = Color(0xFF7C4DFF)
    val mutedText = ColorProvider(day = Color.Black.copy(alpha = 0.35f), night = Color.White.copy(alpha = 0.35f))

    Box(
        modifier = modifier
            .background(primaryColor.copy(alpha = 0.05f))
            .cornerRadius(12.dp),
        contentAlignment = if (isRtl) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isRtl) {
                // Placeholder Stripe Left
                Box(
                    modifier = GlanceModifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(primaryColor.copy(alpha = 0.15f))
                        .cornerRadius(2.dp)
                ) {}
            }

            Column(
                modifier = GlanceModifier.defaultWeight().padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalAlignment = if (isRtl) Alignment.Start else Alignment.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = context.getString(com.example.R.string.widget_no_inactive_slot_title),
                    maxLines = 1,
                    style = TextStyle(
                        color = mutedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = if (isRtl) TextAlign.Start else TextAlign.End
                    )
                )
                Text(
                    text = context.getString(com.example.R.string.widget_no_stop_date),
                    maxLines = 1,
                    style = TextStyle(
                        color = mutedText,
                        fontSize = 8.sp,
                        textAlign = if (isRtl) TextAlign.End else TextAlign.Start
                    )
                )
                Text(
                    text = context.getString(com.example.R.string.widget_no_stop_duration),
                    maxLines = 1,
                    style = TextStyle(
                        color = mutedText,
                        fontSize = 8.sp,
                        textAlign = if (isRtl) TextAlign.End else TextAlign.Start
                    )
                )
            }

            if (isRtl) {
                // Placeholder Stripe Right
                Box(
                    modifier = GlanceModifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(primaryColor.copy(alpha = 0.15f))
                        .cornerRadius(2.dp)
                ) {}
            }
        }
    }
}

/**
 * Formats the timestamp into a localized date string (e.g., 2024/7/12).
 */
private fun getFormattedStopDate(context: Context, timestamp: Long): String {
    val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    val dateStr = String.format(Locale.US, "%04d/%d/%d", date.year, date.monthValue, date.dayOfMonth)
    return context.getString(com.example.R.string.widget_stopped_date, com.example.util.AppFormatters.forceWesternDigits(dateStr))
}

/**
 * Calculates and formats the duration since the habit became inactive.
 * Supports localized Arabic text with proper pluralization rules.
 */
private fun getDetailedInactiveDuration(context: Context, timestamp: Long): String {
    val inactiveInstant = Instant.ofEpochMilli(timestamp)
    val now = Instant.now()
    
    val totalMinutes = ChronoUnit.MINUTES.between(inactiveInstant, now).toInt().coerceAtLeast(0)
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes % (24 * 60)) / 60
    
    val isArabic = context.resources.configuration.locales[0].language == "ar"
    
    return if (isArabic) {
        val daysText = when {
            days == 0 -> ""
            days == 1 -> "يوم واحد"
            days == 2 -> "يومين"
            days <= 10 -> "$days أيام"
            else -> "$days يوماً"
        }
        val hoursText = when {
            hours == 0 -> ""
            hours == 1 -> "ساعة واحدة"
            hours == 2 -> "ساعتين"
            hours <= 10 -> "$hours ساعات"
            else -> "$hours ساعة"
        }
        
        val durationPart = when {
            days > 0 && hours > 0 -> "$daysText و $hoursText"
            days > 0 -> daysText
            hours > 0 -> hoursText
            else -> "أقل من ساعة"
        }
        
        com.example.util.AppFormatters.forceWesternDigits("متوقفة منذ $durationPart")
    } else {
        val daysPart = if (days > 0) "$days days " else ""
        val hoursPart = if (hours > 0) "$hours hours" else ""
        "Inactive for $daysPart$hoursPart"
    }
}

class InactiveHabitsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = InactiveHabitsWidget()
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val app = context.applicationContext as HabitApplication
        app.applicationScope.launch(kotlinx.coroutines.Dispatchers.Main.immediate) {
            HabitWidgetSyncUpdater.updateNowForced(context)
        }
    }
}
