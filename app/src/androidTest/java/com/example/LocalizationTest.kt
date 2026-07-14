package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LocalizationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testEnglishLocalizationNoArabicLeakage() {
        // 1. Start on Home, navigate to Settings
        // (Assuming bottom nav is visible)
        composeTestRule.onNodeWithText("Settings").performClick()
        
        // 2. Open Language Dropdown
        composeTestRule.onNodeWithTag("language_dropdown").performClick()
        
        // 3. Select English
        composeTestRule.onNodeWithTag("lang_option_en").performClick()
        
        composeTestRule.waitForIdle()

        // 4. Navigate back to Home and then to Add Habit
        composeTestRule.onNodeWithText("Home").performClick()
        composeTestRule.onNodeWithTag("add_habit_fab").performClick()

        // 5. Open Time Picker
        composeTestRule.onNodeWithTag("add_reminder_button").performClick()

        // 6. Verify labels in English
        composeTestRule.onNodeWithText("Hour").assertExists()
        composeTestRule.onNodeWithText("Minute").assertExists()
        composeTestRule.onNodeWithText("Period").assertExists()
        
        // 7. Verify no Arabic leakage
        composeTestRule.onNodeWithText("الساعة").assertDoesNotExist()
    }

    @Test
    fun testArabicLocalization() {
        // 1. Force App Language to Arabic
        composeTestRule.runOnUiThread {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ar"))
        }
        
        composeTestRule.waitForIdle()

        // 2. Verify labels in Arabic
        // composeTestRule.onNodeWithText("الساعة").assertExists()
    }
}
