package com.example.overlay.composable

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun HabitOverlayContent(
    habitName:  String,
    habitColor: String,
    habitDesc:  String,
    quote:      String = "",
    onDone:     () -> Unit,
    onDismiss:  () -> Unit
) {
    val accentColor = remember(habitColor) {
        try { Color(android.graphics.Color.parseColor(habitColor)) }
        catch (_: Exception) { Color(0xFF7C4DFF) }
    }

    // Constants for the new larger design
    val cardWidth = 340.dp
    val internalPadding = 20.dp
    val verticalSpacing = 14.dp
    val actionButtonHeight = 44.dp

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "overlay_scale"
    )

    Card(
        modifier = Modifier
            .width(cardWidth)
            .scale(scale),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xF21A1A2E) // 95% opacity dark background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column {

            // Colored top accent bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(accentColor)
            )

            Column(
                modifier = Modifier.padding(internalPadding),
                verticalArrangement = Arrangement.spacedBy(verticalSpacing)
            ) {

                // Title row: colored dot + habit name + close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Text(
                            text = habitName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE8E8F0)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.cancel),
                            tint = Color(0xFF8A8AA0),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Optional description
                if (habitDesc.isNotBlank()) {
                    Text(
                        text = habitDesc,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF8A8AA0)
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Motivational Quote (Part B)
                if (quote.isNotBlank()) {
                    Text(
                        text = "\"$quote\"",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF8A8AA0).copy(alpha = 0.7f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Reminder label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.example.R.string.overlay_time_for_habit),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = accentColor,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(actionButtonHeight),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF8A8AA0)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF2A2A45))
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.example.R.string.overlay_later),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Button(
                        onClick = onDone,
                        modifier = Modifier
                            .weight(1f)
                            .height(actionButtonHeight),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF69F0AE)
                        )
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.example.R.string.overlay_done),
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color(0xFF0F2B1A),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

