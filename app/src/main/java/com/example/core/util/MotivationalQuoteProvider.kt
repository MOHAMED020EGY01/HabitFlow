package com.example.core.util

import android.content.Context
import com.example.R
import com.example.app.HabitApplication
import java.util.Locale
import kotlin.random.Random

class MotivationalQuoteProvider(private val context: Context) {
    
    private var lastQuoteIndex: Int = -1

    fun getRandomQuote(): String {
        val app = context.applicationContext as? HabitApplication
        val language = app?.currentLanguageCode ?: "system"
        
        val resources = if (language != "system") {
            val locale = java.util.Locale.forLanguageTag(language)
            val config = android.content.res.Configuration(context.resources.configuration)
            config.setLocale(locale)
            context.createConfigurationContext(config).resources
        } else {
            context.resources
        }

        val quotes = resources.getStringArray(R.array.motivational_quotes)
        if (quotes.isEmpty()) return ""
        
        if (quotes.size == 1) return quotes[0]

        var nextIndex: Int
        do {
            nextIndex = Random.nextInt(quotes.size)
        } while (nextIndex == lastQuoteIndex)
        
        lastQuoteIndex = nextIndex
        return quotes[nextIndex]
    }
}
