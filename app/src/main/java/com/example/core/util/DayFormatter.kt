package com.example.core.util

import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

object DayFormatter {
    /**
     * Returns a concise day name (abbreviation).
     * For Arabic, it uses a custom mapping to avoid multiple days starting with "Alif"
     * all showing the same "ا" or "أ" character in NARROW style.
     */
    fun getShortDayName(day: DayOfWeek, locale: Locale): String {
        return if (locale.language == "ar") {
            when (day) {
                DayOfWeek.SATURDAY -> "س"
                DayOfWeek.SUNDAY -> "ح"
                DayOfWeek.MONDAY -> "ن"
                DayOfWeek.TUESDAY -> "ث"
                DayOfWeek.WEDNESDAY -> "ر"
                DayOfWeek.THURSDAY -> "خ"
                DayOfWeek.FRIDAY -> "ج"
            }
        } else {
            day.getDisplayName(TextStyle.SHORT, locale)
        }
    }

    /**
     * Returns the full day name (e.g. "Monday", "الاثنين").
     */
    fun getFullDayName(day: DayOfWeek, locale: Locale): String {
        return day.getDisplayName(TextStyle.FULL, locale)
    }
}
