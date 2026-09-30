plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.binary.compatibility.validator) apply false
}

// Hilt's aggregator worker loads JavaPoet parent-first; AGP's bundled jetifier ships
// JavaPoet 1.10.0 (missing ClassName.canonicalName). Pinning 1.13.0 on the root
// buildscript classpath (ancestor of plugin loaders) makes the compatible version win.
buildscript {
    dependencies {
        classpath(libs.javapoet)
    }
}

allprojects {
    group = "com.mori"
    version = "1.0.0"

    // Pulsar 1.3.0 transitively pulls androidx.core:core 1.17.0, which
    // requires AGP 8.9.1+ (repo pins 8.7.3). Force the repo's 1.15.0: it
    // already carries every vibrator/compat API Pulsar's haptics path uses.
    configurations.all {
        resolutionStrategy.force("androidx.core:core:1.15.0", "androidx.core:core-ktx:1.15.0")
    }
}
