package com.example.overlay

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.pendingOverlayDataStore: DataStore<Preferences> by preferencesDataStore(name = "pending_overlay")

class PendingOverlayStore(private val context: Context) {
    private val KEY_HABIT_ID    = intPreferencesKey("pending_overlay_habit_id")
    private val KEY_HABIT_NAME  = stringPreferencesKey("pending_overlay_habit_name")
    private val KEY_HABIT_COLOR = stringPreferencesKey("pending_overlay_habit_color")
    private val KEY_HABIT_DESC  = stringPreferencesKey("pending_overlay_habit_desc")
    private val KEY_TRIGGERED_AT = longPreferencesKey("pending_overlay_triggered_at")

    private val dataStore = context.pendingOverlayDataStore

    suspend fun savePending(
        habitId: Int,
        habitName: String,
        habitColor: String,
        habitDesc: String
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_HABIT_ID] = habitId
            prefs[KEY_HABIT_NAME] = habitName
            prefs[KEY_HABIT_COLOR] = habitColor
            prefs[KEY_HABIT_DESC] = habitDesc
            prefs[KEY_TRIGGERED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun consumePending(): PendingOverlay? {
        val prefs = dataStore.data.first()
        val habitId = prefs[KEY_HABIT_ID] ?: return null

        val result = PendingOverlay(
            habitId = habitId,
            habitName = prefs[KEY_HABIT_NAME] ?: return null,
            habitColor = prefs[KEY_HABIT_COLOR] ?: "#7C4DFF",
            habitDesc = prefs[KEY_HABIT_DESC] ?: "",
            triggeredAt = prefs[KEY_TRIGGERED_AT] ?: 0L
        )

        // Clear it immediately so it never fires twice
        dataStore.edit { it.clear() }
        return result
    }

    data class PendingOverlay(
        val habitId: Int,
        val habitName: String,
        val habitColor: String,
        val habitDesc: String,
        val triggeredAt: Long
    )
}
