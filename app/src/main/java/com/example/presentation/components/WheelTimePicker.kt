package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

import androidx.compose.ui.res.stringResource
import com.example.R

@Composable
fun WheelTimePicker(
    initialHour: Int,
    initialMinute: Int,
    langCode: String? = null,
    onTimeChanged: (Int, Int) -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(if (initialHour % 12 == 0) 12 else initialHour % 12) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }
    var isAm by remember { mutableStateOf(initialHour < 12) }

    val amLabel = stringResource(R.string.am)
    val pmLabel = stringResource(R.string.pm)

    // Resolve isArabic from langCode if provided, otherwise from context
    val isArabic = remember(langCode) { 
        if (langCode != null) langCode == "ar" 
        else com.example.HabitApplication.instance.currentLanguageCode == "ar"
    }

    LaunchedEffect(selectedHour, selectedMinute, isAm) {
        val hour24 = when {
            isAm && selectedHour == 12 -> 0
            isAm -> selectedHour
            !isAm && selectedHour == 12 -> 12
            else -> selectedHour + 12
        }
        onTimeChanged(hour24, selectedMinute)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp), // Increased height to accommodate headers
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hour Wheel
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.hour),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            WheelColumn(
                items = (1..12).toList(),
                initialValue = selectedHour,
                onValueSelected = { selectedHour = it },
                format = { com.example.util.AppFormatters.forceWesternDigits(it.toString()) }
            )
        }

        Text(text = ":", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 24.dp))

        // Minute Wheel
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.minute),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            WheelColumn(
                items = (0..59).toList(),
                initialValue = selectedMinute,
                format = { com.example.util.AppFormatters.forceWesternDigits(String.format(Locale.US, "%02d", it)) },
                onValueSelected = { selectedMinute = it }
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // AM/PM Wheel - Not infinite
        Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.period),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            WheelColumn(
                items = listOf(amLabel, pmLabel),
                initialValue = if (isAm) amLabel else pmLabel,
                onValueSelected = { isAm = it == amLabel },
                isInfinite = false
            )
        }
    }
}

@Composable
fun <T> WheelColumn(
    items: List<T>,
    initialValue: T,
    modifier: Modifier = Modifier,
    isInfinite: Boolean = true,
    format: (T) -> String = { it.toString() },
    onValueSelected: (T) -> Unit
) {
    val totalItems = if (isInfinite) 10000 else items.size
    val initialIndex = if (isInfinite) {
        (totalItems / 2) - (totalItems / 2 % items.size) + items.indexOf(initialValue)
    } else {
        items.indexOf(initialValue)
    }
    
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex
    )

    val selectedIndex by remember {
        derivedStateOf {
            if (isInfinite) {
                listState.firstVisibleItemIndex % items.size
            } else {
                listState.firstVisibleItemIndex
            }
        }
    }

    val itemHeight = 40.dp
    
    // Snap logic
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex
            val actualIndex = if (isInfinite) {
                centerIndex % items.size
            } else {
                centerIndex.coerceIn(0, items.size - 1)
            }
            if (actualIndex in items.indices) {
                onValueSelected(items[actualIndex])
            }
            
            // Adjust scroll position to center the item exactly
            if (listState.firstVisibleItemIndex in 0 until totalItems) {
                listState.animateScrollToItem(listState.firstVisibleItemIndex)
            }
        }
    }

    Box(modifier = modifier.height(itemHeight * 5), contentAlignment = Alignment.Center) {
        // Compact selected pill background (Middle Row)
        Box(
            modifier = Modifier
                .width(72.dp) // wide enough for "صباحًا"
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = if (isInfinite) PaddingValues(vertical = itemHeight * 2) else PaddingValues(vertical = itemHeight * 2),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(totalItems) { index ->
                val actualIndex = if (isInfinite) index % items.size else index
                if (actualIndex in items.indices) {
                    val item = items[actualIndex]
                    val isSelected = selectedIndex == actualIndex

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = format(item),
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.3f),
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
