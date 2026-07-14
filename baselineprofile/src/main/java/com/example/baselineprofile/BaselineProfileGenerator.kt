package com.example.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = "com.aistudio.habittracker.yfnwdp"
    ) {
        pressHome()
        startActivityAndWait()
        device.waitForIdle()

        // 0. Handle Onboarding if present (first-run fallback)
        try {
            val onboardingInput = device.findObject(By.res("onboarding_username_input"))
            if (onboardingInput != null) {
                onboardingInput.text = "Ahmed"
                device.waitForIdle()
                device.findObject(By.res("onboarding_start_button"))?.click()
                device.waitForIdle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 1. Add a new habit end-to-end
        try {
            val addHabitFab = device.findObject(By.res("add_habit_fab"))
            if (addHabitFab != null) {
                addHabitFab.click()
                device.waitForIdle()

                val nameInput = device.findObject(By.res("habit_name_input"))
                if (nameInput != null) {
                    nameInput.text = "Exercise Daily"
                    device.waitForIdle()

                    val descInput = device.findObject(By.res("habit_desc_input"))
                    descInput?.text = "30 minutes of cardio in the morning"
                    device.waitForIdle()

                    // Click Save Button
                    val saveButton = device.findObject(By.res("save_habit_button"))
                    saveButton?.click()
                    device.waitForIdle()
                } else {
                    // Try to click back if we couldn't find the name input
                    device.pressBack()
                    device.waitForIdle()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Click/interact with the "All Habits" tab
        try {
            val allHabitsTab = device.findObject(By.res("nav_item_all_habits")) ?: device.findObject(By.text("All Habits"))
            if (allHabitsTab != null) {
                allHabitsTab.click()
                device.waitForIdle()

                // Interact with filter chips
                val activeFilter = device.findObject(By.res("filter_ACTIVE"))
                activeFilter?.click()
                device.waitForIdle()

                val completeFilter = device.findObject(By.res("filter_COMPLETE"))
                completeFilter?.click()
                device.waitForIdle()

                val allFilter = device.findObject(By.res("filter_ALL"))
                allFilter?.click()
                device.waitForIdle()

                // Interact with sort chips
                val progressSort = device.findObject(By.res("sort_PROGRESS"))
                progressSort?.click()
                device.waitForIdle()

                val startDateSort = device.findObject(By.res("sort_START_DATE"))
                startDateSort?.click()
                device.waitForIdle()

                // Live search: type search query
                val searchInput = device.findObject(By.res("search_input"))
                if (searchInput != null) {
                    searchInput.text = "Exercise"
                    device.waitForIdle()
                    // Clear search to show cards again
                    searchInput.text = ""
                    device.waitForIdle()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Open the "Habit Detail" screen for the newly created habit (or any habit card present)
        try {
            val habitCard = device.findObject(By.res(Pattern.compile(".*habit_card_.*")))
            if (habitCard != null) {
                habitCard.click()
                device.waitForIdle()

                // - Click "Mark today as done"
                val markDoneButton = device.findObject(By.res("mark_today_done_button"))
                markDoneButton?.click()
                device.waitForIdle()

                // - Pause the habit
                val pauseButton = device.findObject(By.res("pause_habit_button"))
                pauseButton?.click()
                device.waitForIdle()

                // - Resume the habit
                val resumeButton = device.findObject(By.res("resume_habit_button"))
                resumeButton?.click()
                device.waitForIdle()

                // - Click back to return to the list
                val backButton = device.findObject(By.res("back_button"))
                if (backButton != null) {
                    backButton.click()
                } else {
                    device.pressBack()
                }
                device.waitForIdle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Click the "Summary" tab and inspect analytics
        try {
            val summaryTab = device.findObject(By.res("nav_item_summary")) ?: device.findObject(By.text("Summary"))
            if (summaryTab != null) {
                summaryTab.click()
                device.waitForIdle()

                // Scroll down to ensure we run baseline compiler on rendering details
                device.swipe(100, 500, 100, 200, 10)
                device.waitForIdle()
                device.swipe(100, 200, 100, 500, 10)
                device.waitForIdle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 5. Click the "Settings" tab and interact with toggles
        try {
            val settingsTab = device.findObject(By.res("nav_item_settings")) ?: device.findObject(By.text("Settings"))
            if (settingsTab != null) {
                settingsTab.click()
                device.waitForIdle()

                // Edit the username
                val usernameInput = device.findObject(By.res("edit_username_input"))
                val saveUsernameButton = device.findObject(By.res("save_username_button"))
                if (usernameInput != null && saveUsernameButton != null) {
                    usernameInput.text = "Ahmed New"
                    device.waitForIdle()
                    saveUsernameButton.click()
                    device.waitForIdle()
                }

                // Toggle theme option (Light, Dark, then back to System)
                val themeLight = device.findObject(By.res("theme_light"))
                themeLight?.click()
                device.waitForIdle()

                val themeDark = device.findObject(By.res("theme_dark"))
                themeDark?.click()
                device.waitForIdle()

                val themeSystem = device.findObject(By.res("theme_system"))
                themeSystem?.click()
                device.waitForIdle()

                // Toggle language option (Arabic, then back to English or System)
                val langAr = device.findObject(By.res("lang_ar"))
                langAr?.click()
                device.waitForIdle()

                val langEn = device.findObject(By.res("lang_en"))
                langEn?.click()
                device.waitForIdle()

                // Toggle "Card animations" switch
                val cardAnimationsToggle = device.findObject(By.res("card_animations_toggle"))
                cardAnimationsToggle?.click()
                device.waitForIdle()
                cardAnimationsToggle?.click()
                device.waitForIdle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
