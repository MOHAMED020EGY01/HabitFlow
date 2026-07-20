package com.example.core.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object AppFormatters {
    /**
     * Force Western digits (0-9) even in Arabic locale.
     */
    fun forceWesternDigits(text: String): String {
        return text.replace('٠', '0')
            .replace('١', '1')
            .replace('٢', '2')
            .replace('٣', '3')
            .replace('٤', '4')
            .replace('٥', '5')
            .replace('٦', '6')
            .replace('٧', '7')
            .replace('٨', '8')
            .replace('٩', '9')
    }

    /**
     * Consistently returns full localized day name.
     */
    fun getFullDayName(day: DayOfWeek, locale: Locale): String {
        return day.getDisplayName(TextStyle.FULL, locale)
    }

    /**
     * Unified week boundary: always starts on Sunday.
     */
    fun getStartOfWeek(date: LocalDate): LocalDate {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    }

    /**
     * Unified week boundary: always ends on Saturday.
     */
    fun getEndOfWeek(date: LocalDate): LocalDate {
        return getStartOfWeek(date).plusDays(6)
    }

    /**
     * Formats time contiguous (e.g. "9:00 AM") with full Arabic period words.
     * Uses explicit [langCode] if provided, otherwise falls back to Application setting.
     */
    fun formatTime(hour: Int, minute: Int, langCode: String? = null): String {
        val app = com.example.app.HabitApplication.instance
        val effectiveLang = langCode ?: app.currentLanguageCode
        
        val localizedCtx = com.example.core.util.LocaleDirectionHelper.getLocalizedContext(app, effectiveLang)
        val amPm = if (hour < 12) {
            localizedCtx.getString(com.example.R.string.am)
        } else {
            localizedCtx.getString(com.example.R.string.pm)
        }

        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        // Using Locale.US to force Western digits for numbers
        return String.format(Locale.US, "%d:%02d %s", displayHour, minute, amPm)
    }

    /**
     * Overload for LocalTime
     */
    fun formatTime(time: LocalTime, langCode: String? = null): String {
        return formatTime(time.hour, time.minute, langCode)
    }

    /**
     * Legacy support
     */
    fun formatTime(hour: Int, minute: Int, isArabic: Boolean): String {
        return formatTime(hour, minute, if (isArabic) "ar" else "en")
    }
    fun formatTime(time: LocalTime, isArabic: Boolean): String {
        return formatTime(time, if (isArabic) "ar" else "en")
    }
    
    /**
     * Formats date forcing Western digits.
     */
    fun formatDate(date: LocalDate, pattern: String, langCode: String? = null): String {
        val app = com.example.app.HabitApplication.instance
        val effectiveLang = langCode ?: app.currentLanguageCode
        val locale = com.example.core.util.LocaleDirectionHelper.getLocale(effectiveLang)
        val formatted = date.format(DateTimeFormatter.ofPattern(pattern, locale))
        return forceWesternDigits(formatted)
    }

    /**
     * Legacy support
     */
    fun formatDate(date: LocalDate, pattern: String, locale: Locale): String = formatDate(date, pattern)
}
