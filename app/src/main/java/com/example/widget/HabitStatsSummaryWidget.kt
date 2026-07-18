package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextAlign
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxWidth
import com.example.R
import com.example.HabitApplication
import com.example.MainActivity
import com.example.presentation.navigation.Routes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class HabitStatsSummaryWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as HabitApplication

        var stats = HabitWidgetRepository.HabitStatsSummary(0, 0, 0, 0, 0, "None", 0, "#7C4DFF")
        var langCode = "system"
        var hadError = false

        try {
            app.ensureInitialized(timeoutMs = 2000L)
            val repository = HabitWidgetRepository(app.repository)
            stats = repository.getHabitStatsSummary()

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
            HabitStatsSummaryWidgetContent(
                context = localizedContext,
                stats = stats,
                hadError = hadError
            )
        }
    }
}

@Composable
private fun HabitStatsSummaryWidgetContent(
    context: Context,
    stats: HabitWidgetRepository.HabitStatsSummary,
    hadError: Boolean
) {
    val backgroundColor = ColorProvider(day = Color(0xFFF3F7EF), night = Color(0xFF0F0F1A))
    val glassTint = ColorProvider(day = Color(0xFF5E35B1).copy(alpha = 0.05f), night = Color(0xFF7C4DFF).copy(alpha = 0.15f))
    val borderTint = ColorProvider(day = Color.Black.copy(alpha = 0.08f), night = Color.White.copy(alpha = 0.15f))

    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra("DEEP_LINK_ROUTE", Routes.SUMMARY)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(intent))
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(borderTint)
                .cornerRadius(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(1.2.dp)
                    .background(backgroundColor)
                    .cornerRadius(23.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(glassTint)
                        .cornerRadius(23.dp)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (hadError) {
                        Text(
                            text = context.getString(R.string.widget_failed_load),
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
                            // Title Pill
                            Box(
                                modifier = GlanceModifier
                                    .background(ColorProvider(day = Color(0xFF7C4DFF).copy(alpha = 0.2f), night = Color(0xFF7C4DFF).copy(alpha = 0.3f)))
                                    .cornerRadius(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = context.getString(R.string.widget_stats_summary_title),
                                    style = TextStyle(
                                        color = ColorProvider(day = Color(0xFF7C4DFF), night = Color.White),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = GlanceModifier.padding(horizontal = 18.dp, vertical = 5.dp)
                                )
                            }

                            Spacer(modifier = GlanceModifier.height(2.dp))

                            // Best Streak Card (Top, Full Width)
                            BestStreakCard(
                                habitName = stats.bestStreakName,
                                streakCount = stats.bestStreakCount,
                                label = context.getString(R.string.summary_best_streak).replace(" 🔥", ""),
                                accentColor = try { Color(android.graphics.Color.parseColor(stats.bestStreakColor)) } catch (e: Exception) { Color(0xFF009688) }
                            )

                            Spacer(modifier = GlanceModifier.height(8.dp))

                            // 2x2 Grid
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Row(modifier = GlanceModifier.fillMaxWidth()) {
                                    StatCard(
                                        count = stats.completedCount,
                                        label = context.getString(R.string.widget_stat_completed),
                                        accentColor = Color(0xFF4CAF50), // Green
                                        modifier = GlanceModifier.defaultWeight()
                                    )
                                    Spacer(modifier = GlanceModifier.width(4.dp))
                                    StatCard(
                                        count = stats.totalCount,
                                        label = context.getString(R.string.widget_stat_total),
                                        accentColor = Color(0xFF7C4DFF), // Purple
                                        modifier = GlanceModifier.defaultWeight()
                                    )
                                }
                                Spacer(modifier = GlanceModifier.height(8.dp))
                                Row(modifier = GlanceModifier.fillMaxWidth()) {
                                    StatCard(
                                        count = stats.activeCount,
                                        label = context.getString(R.string.widget_stat_active),
                                        accentColor = Color(0xFF43A047), // Darker Green
                                        modifier = GlanceModifier.defaultWeight()
                                    )
                                    Spacer(modifier = GlanceModifier.width(4.dp))
                                    StatCard(
                                        count = stats.inactiveCount,
                                        label = context.getString(R.string.widget_stat_inactive),
                                        accentColor = Color(0xFFE53935), // Red
                                        modifier = GlanceModifier.defaultWeight()
                                    )
                                }
                            }

                            // Failed Card (Bottom, Full Width)
                            StatCard(
                                count = stats.failedCount,
                                label = context.getString(R.string.filter_failure),
                                accentColor = Color(0xFFD32F2F), // Dark Red
                                modifier = GlanceModifier.fillMaxWidth() // Large width for bottom
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BestStreakCard(
    habitName: String,
    streakCount: Int,
    label: String,
    accentColor: Color
) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(accentColor.copy(alpha = 0.15f))
            .cornerRadius(12.dp)
            .padding(8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            // Streak Icon
            Box(
                modifier = GlanceModifier
                    .background(accentColor.copy(alpha = 0.2f))
                    .cornerRadius(8.dp)
                    .padding(6.dp)
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_habit_notification),
                    contentDescription = null,
                    modifier = GlanceModifier.width(16.dp).height(16.dp),
                    colorFilter = androidx.glance.ColorFilter.tint(ColorProvider(accentColor, accentColor)),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = GlanceModifier.width(10.dp))

            Column {
                Text(
                    text = habitName,
                    style = TextStyle(
                        color = ColorProvider(day = Color.Black, night = Color.White),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "$label - $streakCount days",
                    style = TextStyle(
                        color = ColorProvider(accentColor, accentColor),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    count: Int,
    label: String,
    accentColor: Color,
    modifier: GlanceModifier
) {
    Box(
        modifier = modifier
            .background(accentColor.copy(alpha = 0.12f))
            .cornerRadius(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = GlanceModifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = count.toString(),
                style = TextStyle(
                    color = ColorProvider(accentColor, accentColor),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            Text(
                text = label,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(day = Color(0xFF1F2E20).copy(alpha = 0.8f), night = Color.White.copy(alpha = 0.8f)),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

class HabitStatsSummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitStatsSummaryWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val app = context.applicationContext as HabitApplication
        app.applicationScope.launch(kotlinx.coroutines.Dispatchers.Main.immediate) {
            HabitWidgetSyncUpdater.updateNowForced(context)
        }
    }
}
