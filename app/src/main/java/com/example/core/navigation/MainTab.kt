package com.example.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
enum class MainTab(
    @param:StringRes val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(com.example.R.string.menu_home, Icons.Filled.Home, Icons.Outlined.Home),
    ALL_HABITS(com.example.R.string.menu_all_habits, Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List),
    SUMMARY(com.example.R.string.menu_summary, Icons.Filled.Star, Icons.Outlined.Star),
    SETTINGS(com.example.R.string.menu_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}
