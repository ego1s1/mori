# Mori app ProGuard / R8 rules.
#
# Minification is currently OFF (see app/build.gradle.kts) until release builds are
# verified on device. These rules are in place for the day it turns on, covering the
# reflection/codegen-heavy dependencies. Hilt, Room, Coil, and kotlinx-serialization
# each ship their own consumer rules as well; the entries below close the gaps.

# Keep serializable navigation routes (type-safe Navigation uses them reflectively).
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Hilt entry points and injected members referenced by generated components.
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep class * extends androidx.activity.ComponentActivity

# Room entities and DAOs referenced by generated implementations.
-keep class com.mori.core.database.** { *; }

# Navigation type-safe routes.
-keep class com.mori.feature.**.api.** { *; }

# Tesseract JNI bridge (arm64 native library in comic-ocr).
-keep class dev.ffmpegkit.tesseract.** { *; }
