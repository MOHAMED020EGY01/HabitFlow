########################################
# HabitFlow - ProGuard / R8 Rules
########################################

########################################
# Application
########################################

-keep class com.example.app.HabitApplication { *; }
-keep class com.example.app.MainActivity { *; }

########################################
# Android Components
########################################

-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider
-keep class * extends android.appwidget.AppWidgetProvider

########################################
# Glance AppWidget
########################################

-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver
-keep class androidx.glance.** { *; }

########################################
# WorkManager
########################################

-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

########################################
# Room
########################################

-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

########################################
# Gson
########################################

-keepattributes Signature
-keepattributes *Annotation*

########################################
# Coroutines
########################################

-dontwarn kotlinx.coroutines.**

########################################
# Compose
########################################

-dontwarn androidx.compose.**

########################################
# Kotlin Metadata
########################################

-keep class kotlin.Metadata { *; }

########################################
# Enums
########################################

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

########################################
# Serializable
########################################

-keepnames class * implements java.io.Serializable

########################################
# Keep Runtime Annotations
########################################

-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes AnnotationDefault
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Signature
