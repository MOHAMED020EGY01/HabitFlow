package com.example.core.infrastructure.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextAlign
import com.example.app.HabitApplication
import com.example.app.MainActivity
import com.example.core.infrastructure.widget.util.CircularProgressRingRenderer
import com.example.core.infrastructure.widget.util.EmptySlotCircleRenderer
import com.example.core.infrastructure.widget.util.CheckIconRenderer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

import android.util.Log
import com.example.BuildConfig

class AllHabitsWidget : GlanceAppWidget() {
    companion object {
        private const val TAG = "AllHabitsWidget_DIAG"
    }

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d(TAG, "[1] provideGlance started | id: $id")
        val app = context.applicationContext as HabitApplication
        
        try {
            app.ensureInitialized(timeoutMs = 2000L)
            Log.d(TAG, "[2] ensureInitialized success")
        } catch (e: Exception) {
            Log.e(TAG, "[2] ensureInitialized FAILED", e)
        }

        val repository = try {
            HabitWidgetRepository(app.repository)
        } catch (e: Exception) {
            Log.e(TAG, "[3] Repository creation FAILED", e)
            null
        }

        val habitDataList = try {
            val data = repository?.getTopActiveHabitsForWidgets() ?: emptyList()
            Log.d(TAG, "[4] habitDataList size: ${data.size}")
            data
        } catch (e: Exception) {
            Log.e(TAG, "[4] getTopActiveHabitsForWidgets FAILED", e)
            emptyList()
        }

        val langCode = try {
            val code = app.preferencesManager.appLanguageFlow.first()
            Log.d(TAG, "[5] langCode: $code")
            code
        } catch (e: Exception) {
            Log.e(TAG, "[5] langCode read FAILED", e)
            "system"
        }
        
        val localizedContext = try {
            com.example.core.util.LocaleDirectionHelper.getLocalizedContext(context, langCode)
        } catch (e: Exception) {
            Log.e(TAG, "[6] Localization FAILED", e)
            context
        }

        provideContent {
            Log.d(TAG, "[7] provideContent composition starting")
            AllHabitsWidgetContent(
                context = localizedContext,
                habitDataList = habitDataList,
                langCode = langCode
            )
            Log.d(TAG, "[7] provideContent composition finished")
        }
    }
}

@Composable
private fun AllHabitsWidgetContent(
    context: Context,
    habitDataList: List<HabitWidgetRepository.WidgetHabitData>,
    langCode: String
) {
    Log.d("AllHabitsWidget_DIAG", "[8] Rendering AllHabitsWidgetContent")
    
    val primaryColor = Color(0xFF7C4DFF)
    val backgroundColor = Color(0xFF0F0F1A)
    val glassTint = primaryColor.copy(alpha = 0.15f)
    val borderTint = Color.White.copy(alpha = 0.15f)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(borderTint)
            .cornerRadius(20.dp)
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(1.dp)
                .background(backgroundColor)
                .cornerRadius(19.dp)
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(glassTint)
                    .cornerRadius(19.dp)
                    .padding(8.dp)
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title
                    Box(
                        modifier = GlanceModifier
                            .background(ColorProvider(day = Color(0xFF7C4DFF).copy(alpha = 0.2f), night = Color(0xFF7C4DFF).copy(alpha = 0.3f)))
                            .cornerRadius(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = context.getString(com.example.R.string.widget_active_habits_title),
                            style = TextStyle(
                                color = ColorProvider(day = Color(0xFF7C4DFF), night = Color.White),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            modifier = GlanceModifier.padding(horizontal = 18.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(3.dp))
                    
                    for (rowIndex in 0..1) {
                        if (rowIndex > 0) {
                            Spacer(modifier = GlanceModifier.height(5.dp))
                        }
                        Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                            for (colIndex in 0..2) {
                                if (colIndex > 0) {
                                    Spacer(modifier = GlanceModifier.width(5.dp))
                                }
                                val slotIndex = rowIndex * 3 + colIndex
                                val habitData = habitDataList.getOrNull(slotIndex)
                                
                                val habitColor = if (habitData != null) {
                                    try {
                                        Color(android.graphics.Color.parseColor(habitData.colorHex))
                                    } catch (e: Exception) {
                                        Log.e("AllHabitsWidget_DIAG", "Color parse error: ${habitData.colorHex}", e)
                                        Color(0xFF7C4DFF)
                                    }
                                } else {
                                    Color.White
                                }

                                val slotBackground = if (habitData != null) {
                                    habitColor.copy(alpha = 0.12f)
                                } else {
                                    Color.White.copy(alpha = 0.05f)
                                }

                                Box(
                                    modifier = GlanceModifier
                                        .defaultWeight()
                                        .fillMaxHeight()
                                        .background(slotBackground)
                                        .cornerRadius(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (habitData != null) {
                                        HabitSlotContent(
                                            context = context,
                                            habitData = habitData,
                                            langCode = langCode
                                        )
                                    } else {
                                        EmptyHabitSlotContent(context = context)
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

class AllHabitsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AllHabitsWidget()
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val app = context.applicationContext as HabitApplication
        app.applicationScope.launch(kotlinx.coroutines.Dispatchers.Main.immediate) {
            HabitWidgetSyncUpdater.updateNowForced(context)
        }
    }
}

@Composable
internal fun HabitSlotContent(
    context: Context,
    habitData: HabitWidgetRepository.WidgetHabitData,
    langCode: String
) {
    val ringSize = 65.dp
    
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity(buildHabitDetailOrAddIntent(context, habitData.habitId)))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val density = context.resources.displayMetrics.density
            val sizePx = (ringSize.value * density).roundToInt()
            val strokeWidthPx = 7f * density

            val bitmap = remember(habitData.progressPercent, habitData.colorHex) {
                CircularProgressRingRenderer.render(
                    context = context,
                    progressPercent = habitData.progressPercent,
                    colorHex = habitData.colorHex,
                    sizePx = sizePx,
                    strokeWidthPx = strokeWidthPx
                )
            }

            Box(contentAlignment = Alignment.Center) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.size(ringSize)
                )
                Text(
                    text = com.example.core.util.AppFormatters.forceWesternDigits("${habitData.progressPercent.roundToInt()}%"),
                    style = TextStyle(
                        color = ColorProvider(day = Color.White, night = Color.White),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = habitData.name,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(day = Color(0xFFE8E8F0), night = Color(0xFFE8E8F0)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )

            val mutedWhite = Color.White.copy(alpha = 0.7f)
            Text(
                text = com.example.core.util.AppFormatters.forceWesternDigits(
                    context.getString(
                        com.example.R.string.widget_days_left,
                        habitData.daysCompleted,
                        habitData.totalDays,
                        habitData.daysRemaining
                    )
                ),
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(day = mutedWhite, night = mutedWhite),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = GlanceModifier.height(5.dp))

            val today = java.time.LocalDate.now().dayOfWeek
            val isScheduledToday = today in habitData.activeDays

            if (habitData.isCompletedToday) {
                val successColor = Color(0xFF69F0AE)
                val checkBitmap = remember {
                    CheckIconRenderer.render(context, "#69F0AE", (12 * density).roundToInt())
                }
                Row(
                    modifier = GlanceModifier
                        .background(successColor.copy(alpha = 0.12f))
                        .cornerRadius(6.dp)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(checkBitmap),
                        contentDescription = null,
                        modifier = GlanceModifier.size(12.dp)
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = context.getString(com.example.R.string.widget_done_today),
                        style = TextStyle(
                            color = ColorProvider(day = successColor, night = successColor),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            } else if (!isScheduledToday) {
                val mutedGray = Color.White.copy(alpha = 0.38f)
                Box(
                    modifier = GlanceModifier
                        .background(Color.White.copy(alpha = 0.08f))
                        .cornerRadius(6.dp)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getNextActiveDayLocalized(context, habitData, langCode),
                        style = TextStyle(
                            color = ColorProvider(day = mutedGray, night = mutedGray),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            } else {
                val habitColor = try {
                    Color(android.graphics.Color.parseColor(habitData.colorHex))
                } catch (e: Exception) {
                    Color(0xFF7C4DFF)
                }
                Box(
                    modifier = GlanceModifier
                        .background(habitColor.copy(alpha = 0.15f))
                        .cornerRadius(6.dp)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .clickable(
                            actionRunCallback<MarkHabitDoneAction>(
                                androidx.glance.action.actionParametersOf(
                                    MarkHabitDoneAction.habitIdKey to habitData.habitId
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = context.getString(com.example.R.string.widget_mark_done_question),
                        style = TextStyle(
                            color = ColorProvider(day = habitColor, night = habitColor),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
internal fun EmptyHabitSlotContent(context: Context) {
    val ringSize = 65.dp

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(buildHabitDetailOrAddIntent(context, null)))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val density = context.resources.displayMetrics.density
        val sizePx = (ringSize.value * density).roundToInt()
        val strokeWidthPx = 7f * density

        val bitmap = remember {
            EmptySlotCircleRenderer.render(
                context = context,
                sizePx = sizePx,
                strokeWidthPx = strokeWidthPx
            )
        }

        Image(
            provider = ImageProvider(bitmap),
            contentDescription = null,
            modifier = GlanceModifier.size(ringSize)
        )

        Spacer(modifier = GlanceModifier.height(5.dp))

        val mutedGray = Color(0xFF8A8AA0)
        Text(
            text = context.getString(com.example.R.string.widget_no_active),
            style = TextStyle(
                color = ColorProvider(day = mutedGray, night = mutedGray),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = GlanceModifier.height(2.dp))

        val primaryColor = Color(0xFF7C4DFF)
        Text(
            text = context.getString(com.example.R.string.widget_tap_add),
            style = TextStyle(
                color = ColorProvider(day = primaryColor, night = primaryColor),
                fontSize = 9.sp,
                textAlign = TextAlign.Center
            )
        )
    }
}

internal fun buildHabitDetailOrAddIntent(context: Context, habitId: Int?): Intent {
    return if (habitId != null) {
        Intent(context, MainActivity::class.java).apply {
            putExtra("DEEP_LINK_ROUTE", "habit_detail/$habitId")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    } else {
        Intent(context, MainActivity::class.java).apply {
            putExtra("DEEP_LINK_ROUTE", "add_habit")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }
}

private fun getNextActiveDayLocalized(context: Context, habitData: HabitWidgetRepository.WidgetHabitData, langCode: String): String {
    val activeDays = habitData.activeDays
    if (activeDays.isEmpty()) return ""
    
    val today = java.time.LocalDate.now()
    var nextActiveDay: java.time.DayOfWeek? = null
    for (i in 1..7) {
        val nextDate = today.plusDays(i.toLong())
        if (nextDate.dayOfWeek in activeDays) {
            nextActiveDay = nextDate.dayOfWeek
            break
        }
    }
    
    if (nextActiveDay == null) return ""

    val locale = com.example.core.util.LocaleDirectionHelper.getLocale(langCode)
    val dayName = com.example.core.util.DayFormatter.getFullDayName(nextActiveDay, locale)
    
    return context.getString(com.example.R.string.widget_wait_until, dayName)
}
