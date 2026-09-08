plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.detekt) apply false
}

allprojects {
    group = "com.mori"
    version = "0.1.0-SNAPSHOT"
}
