package com.example.feature.habit.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.core.ui.HabitCard
import com.example.core.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllHabitsScreen(
    navController: NavController,
    viewModel: AllHabitsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTime by remember { mutableStateOf(java.time.LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            currentTime = java.time.LocalDateTime.now()
        }
    }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow<String?>("habit_save_result", null)?.collect { message ->
            if (message != null) {
                snackbarHostState.showSnackbar(message)
                savedStateHandle.remove<String>("habit_save_result")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is AllHabitsUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
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
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(com.example.R.string.my_habits),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Live Search Input Box
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text(stringResource(com.example.R.string.search_habits_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(com.example.R.string.search)
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(com.example.R.string.clear_search)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Row filter Tabs (All, Active, Inactive, Complete, Failure) - Horizontally Scrollable for all form factors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HabitFilter.values().forEach { filterType ->
                    val isSelected = uiState.filter == filterType
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable { viewModel.setFilter(filterType) }
                            .padding(horizontal = 16.dp)
                            .testTag("filter_${filterType.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (filterType) {
                                HabitFilter.ALL -> stringResource(com.example.R.string.filter_all)
                                HabitFilter.ACTIVE -> stringResource(com.example.R.string.filter_active)
                                HabitFilter.INACTIVE -> stringResource(com.example.R.string.filter_inactive)
                                HabitFilter.COMPLETE -> stringResource(com.example.R.string.filter_complete)
                                HabitFilter.FAILURE -> stringResource(com.example.R.string.filter_failure)
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sort Toggle Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(com.example.R.string.sort_by),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        HabitSort.START_DATE to com.example.R.string.sort_start_date,
                        HabitSort.PROGRESS to com.example.R.string.sort_progress
                    ).forEach { (sortType, labelRes) ->
                        val isSelected = uiState.sortBy == sortType
                        val label = stringResource(labelRes)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSortBy(sortType) },
                            label = { Text(text = label, fontSize = 12.sp) },
                            modifier = Modifier.testTag("sort_${sortType.name}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lazy Habits List
            if (uiState.habitsWithProgress.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(com.example.R.string.no_habits_match),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val habitsCount = uiState.habitsWithProgress.size
                
                // تحسين نظام مراقبة التمرير باستخدام derivedStateOf (لأعلى أداء)
                val shouldLoadMore by remember {
                    derivedStateOf {
                        val lastVisibleItem = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()
                        lastVisibleItem != null && lastVisibleItem.index >= habitsCount - 2
                    }
                }

                LaunchedEffect(shouldLoadMore) {
                    if (shouldLoadMore && uiState.hasMore) {
                        viewModel.loadMore()
                    }
                }

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = uiState.habitsWithProgress,
                        key = { it.habit.id },
                        contentType = { "HabitCard" }
                    ) { item ->
                        val isChecked = uiState.isCheckedTodayMap[item.habit.id] ?: false
                        val onCheckedToggle = remember(item.habit.id, viewModel) {
                            { checked: Boolean -> viewModel.toggleCheckIn(item.habit.id, checked) }
                        }
                        val onClick = remember(item.habit.id, navController) {
                            { navController.navigate(Routes.HABIT_DETAIL.replace("{habitId}", item.habit.id.toString())) }
                        }
                        val onActivateClick = remember(item.habit.id, viewModel) {
                            { viewModel.activateHabit(item.habit.id) }
                        }
                        HabitCard(
                            habit = item.habit,
                            completedDays = item.completedDays,
                            isCheckedToday = isChecked,
                            onCheckedToggle = onCheckedToggle,
                            onClick = onClick,
                            onActivateClick = onActivateClick,
                            modifier = Modifier
                                .animateItem()
                                .testTag("habit_card_${item.habit.id}"),
                            currentTime = currentTime,
                            isScrolling = lazyListState.isScrollInProgress
                        )
                    }

                    if (uiState.hasMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
