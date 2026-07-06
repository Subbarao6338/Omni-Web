# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve JavaScript Interface methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Preserve Room entities and DAOs
-keep class com.omniweb.app.data.** { *; }

# Preserve WebAppInterface specifically as it's heavily used by JS
-keep class com.omniweb.app.util.WebAppInterface { *; }

# General Compose rules
-keep class androidx.compose.material.icons.** { *; }

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Suppress warnings for missing java.beans classes (often used by libraries like SnakeYAML)
-dontwarn java.beans.**
