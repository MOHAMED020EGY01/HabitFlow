package com.example.presentation.screens.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.domain.usecase.LeaderboardItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    navController: NavController,
    viewModel: SummaryViewModel = viewModel()
) {
    val summary by viewModel.summaryState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val currentLang = remember(context) { 
        context.resources.configuration.locales.get(0).language 
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { innerPadding ->
        val screenBgModifier = if (androidx.compose.foundation.isSystemInDarkTheme())
            Modifier.background(MaterialTheme.colorScheme.background)
        else
            Modifier.background(com.example.ui.theme.LightBackgroundGradientBrush)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(screenBgModifier)
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.summary_analytics),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (summary == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val sum = summary!!

                // Bento Statistics Layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Total Habits Card
                    com.example.presentation.components.GlassCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = com.example.util.AppFormatters.forceWesternDigits("${sum.totalHabits}"),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.example.R.string.summary_total_habits),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Completed Habits Card
                    com.example.presentation.components.GlassCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = com.example.util.AppFormatters.forceWesternDigits("${sum.completedHabits}"),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.example.R.string.summary_fully_completed),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Best Streak Banner Box
                val streakColor = remember {
                    try {
                        Color(android.graphics.Color.parseColor(sum.bestStreakColorHex))
                    } catch (e: Exception) {
                        Color(0xFF7C4DFF)
                    }
                }

                com.example.presentation.components.GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    habitColor = streakColor,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.example.R.string.summary_best_streak),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = sum.bestStreakHabitName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = com.example.util.AppFormatters.forceWesternDigits("${sum.bestStreakCount} Days"),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = streakColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Custom completion rate chart
                CompletionRateChart(items = sum.leaderboard, langCode = currentLang)

                Spacer(modifier = Modifier.height(24.dp))

                // Habit Leaderboard
                com.example.presentation.components.GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.example.R.string.summary_leaderboard),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (sum.leaderboard.isEmpty()) {
                            Text(
                                text = "No habits to rank yet.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                sum.leaderboard.forEach { item ->
                                    val itemColor = remember(item.habit.id, item.habit.colorHex) {
                                        try {
                                            Color(android.graphics.Color.parseColor(item.habit.colorHex))
                                        } catch (e: Exception) {
                                            Color(0xFF7C4DFF)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.background)
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(itemColor),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = com.example.util.AppFormatters.forceWesternDigits("#${item.rank}"),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = item.habit.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        val currentLabel = context.getString(com.example.R.string.summary_completed, item.completedDaysCount)
                                        Text(
                                            text = com.example.util.AppFormatters.forceWesternDigits(currentLabel),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun CompletionRateChart(
    items: List<LeaderboardItem>,
    langCode: String,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    com.example.presentation.components.GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.summary_completion_rates),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items.take(5).forEach { item ->
                    val scheduledTotal = item.habit.getScheduledDaysCount()
                    val progress = if (scheduledTotal > 0) {
                        item.completedDaysCount.toFloat() / scheduledTotal
                    } else 0f

                    val habitColor = remember {
                        try {
                            Color(android.graphics.Color.parseColor(item.habit.colorHex))
                        } catch (e: Exception) {
                            Color(0xFF7C4DFF)
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.habit.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = com.example.util.AppFormatters.forceWesternDigits("${(progress * 100).toInt()}%"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = habitColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(habitColor)
                            )
                        }
                    }
                }
            }
        }
    }
}
