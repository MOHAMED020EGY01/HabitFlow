package com.example.core.util

import android.content.Context
import java.util.Locale

object LocaleDirectionHelper {
    fun isRtl(langCode: String): Boolean {
        val effectiveLang = if (langCode == "system") {
            val systemLocale = androidx.core.os.ConfigurationCompat.getLocales(android.content.res.Resources.getSystem().configuration).get(0)
            systemLocale?.language ?: "en"
        } else {
            langCode
        }
        return effectiveLang == "ar"
    }

    fun getLocale(langCode: String): Locale {
        val effectiveLang = if (langCode == "system") {
            val systemLocale = androidx.core.os.ConfigurationCompat.getLocales(android.content.res.Resources.getSystem().configuration).get(0)
            systemLocale?.language ?: "en"
        } else {
            langCode
        }
        return Locale(effectiveLang)
    }

    fun getLayoutDirection(langCode: String): androidx.compose.ui.unit.LayoutDirection {
        return if (isRtl(langCode)) {
            androidx.compose.ui.unit.LayoutDirection.Rtl
        } else {
            androidx.compose.ui.unit.LayoutDirection.Ltr
        }
    }

    fun getLocalizedContext(context: Context, langCode: String): Context {
        val locale = getLocale(langCode)
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
