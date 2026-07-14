package com.example

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Macrobenchmark tests measuring cold-start time and frame-timing jank
 * on a real RMX3760 device (Android 15).
 *
 * These tests install alongside the app APK and use
 * [MacrobenchmarkRule] to launch, interact, and measure performance.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HabitFlowBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    private val packageName = "com.aistudio.habittracker.yfnwdp"

    // ── Helpers ──────────────────────────────────────────────

    /** Handle first-run onboarding if the splash screen forwards there. */
    private fun MacrobenchmarkScope.dismissOnboardingIfPresent() {
        try {
            val input = device.findObject(By.res(packageName, "onboarding_username_input"))
            if (input != null) {
                input.text = "TestUser"
                device.waitForIdle()
                device.findObject(By.res(packageName, "onboarding_start_button"))?.click()
                device.waitForIdle(1_000)
            }
        } catch (_: Exception) { /* onboarding not shown */ }
    }

    /**
     * Navigate: Home → All Habits → tap first card → Habit Detail → back → Home
     * Repeated [times] times.
     */
    private fun MacrobenchmarkScope.navigateThreeScreens(times: Int = 3) {
        for (i in 1..times) {
            // Tap "See All" to go to All Habits
            try {
                val seeAll = device.findObject(By.res(packageName, "see_all_button"))
                if (seeAll != null) {
                    seeAll.click()
                    device.waitForIdle(1_500)
                } else {
                    val allHabitsTab = device.findObject(By.res(packageName, "nav_item_all_habits"))
                    allHabitsTab?.click()
                    device.waitForIdle(1_500)
                }
            } catch (_: Exception) { /* skip */ }

            // Tap first habit card
            try {
                val habitCard = device.findObject(By.res(packageName, "habit_card_1"))
                if (habitCard == null) {
                    // Try any habit card via regex-like search
                    val allCards = device.findObjects(By.res(packageName, "habit_card_1"))
                    if (allCards.isNotEmpty()) {
                        allCards[0].click()
                        device.waitForIdle(1_500)
                    }
                } else {
                    habitCard.click()
                    device.waitForIdle(1_500)
                }
            } catch (_: Exception) { /* skip */ }

            // Navigate back to Home
            try {
                val backBtn = device.findObject(By.res(packageName, "back_button"))
                if (backBtn != null) {
                    backBtn.click()
                    device.waitForIdle(1_000)
                } else {
                    device.pressBack()
                    device.waitForIdle(1_000)
                }
            } catch (_: Exception) {
                device.pressBack()
                device.waitForIdle(1_000)
            }

            // Press back again to return to Home
            try {
                device.pressBack()
                device.waitForIdle(1_000)
            } catch (_: Exception) { /* skip */ }
        }
    }

    // ── Scroll helper using swipe gestures ───────────────────

    private fun MacrobenchmarkScope.scrollHabitList() {
        try {
            val w = device.displayWidth
            val h = device.displayHeight
            // Scroll down
            device.swipe(w / 2, (h * 0.6).toInt(), w / 2, (h * 0.3).toInt(), 15)
            device.waitForIdle(500)
            // Scroll up
            device.swipe(w / 2, (h * 0.3).toInt(), w / 2, (h * 0.6).toInt(), 15)
            device.waitForIdle(500)
            // Scroll down again
            device.swipe(w / 2, (h * 0.6).toInt(), w / 2, (h * 0.3).toInt(), 15)
            device.waitForIdle(500)
        } catch (_: Exception) { /* skip scrolling */ }
    }

    // ── Test 1: Cold-start time ──────────────────────────────

    @Test
    fun startupTime() {
        benchmarkRule.measureRepeated(
            packageName = packageName,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = CompilationMode.DEFAULT,
            iterations = 5,
            startupMode = StartupMode.COLD,
            setupBlock = {
                // No pre-launch setup – measure full cold start
            }
        ) {
            // This block runs inside the startup measurement window.
            // We just need the first frame to appear.
            startActivityAndWait()

            // Wait for Home screen (or onboarding) to be idle
            device.waitForIdle(3_000)

            dismissOnboardingIfPresent()

            // Confirm Home is reachable
            device.wait(Until.hasObject(By.res(packageName, "add_habit_fab")), 3_000)
        }
    }

    // ── Test 2: Frame-timing during navigation ───────────────

    @Test
    fun frameTiming() {
        benchmarkRule.measureRepeated(
            packageName = packageName,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.DEFAULT,
            iterations = 3,
            startupMode = StartupMode.COLD,
            setupBlock = {
                // Start fresh
            }
        ) {
            startActivityAndWait()
            device.waitForIdle(3_000)

            dismissOnboardingIfPresent()

            // Wait for Home screen to settle
            device.wait(Until.hasObject(By.res(packageName, "add_habit_fab")), 5_000)

            // Scroll the habit list
            scrollHabitList()

            // Navigate through screens
            navigateThreeScreens(times = 3)
        }
    }
}
