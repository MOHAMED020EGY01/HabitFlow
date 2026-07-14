########################################
# HabitFlow - ProGuard / R8 Rules
########################################

########################################
# Application
########################################

-keep class com.example.HabitApplication { *; }
-keep class com.example.MainActivity { *; }

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
# Moshi
########################################

-keep @com.squareup.moshi.JsonClass class * { *; }

-keepclassmembers class * {
    @com.squareup.moshi.* <methods>;
    @com.squareup.moshi.* <fields>;
}

########################################
# Gson
########################################

-keepattributes Signature
-keepattributes *Annotation*

########################################
# Retrofit / OkHttp
########################################

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

########################################
# Coroutines
########################################

-dontwarn kotlinx.coroutines.**

########################################
# Firebase
########################################

-dontwarn com.google.firebase.**
-keep class com.google.firebase.** { *; }

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