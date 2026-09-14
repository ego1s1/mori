plugins {
    alias(libs.plugins.mori.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.mori.android.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.mori.hilt)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.mori.app"
    defaultConfig {
        applicationId = "com.mori.reader"
        // Version precedence: explicit -PappVersionName/-PappVersionCode (used by the
        // release workflow) win; otherwise every commit gets an incremental 1.1.x
        // build derived from the git commit count, e.g. 1.1.23.
        val commitCount = gitCommitCount()
        versionCode = (project.findProperty("appVersionCode") as String?)
            ?.toIntOrNull() ?: commitCount.coerceAtLeast(1)
        versionName = (project.findProperty("appVersionName") as String?)
            ?.removePrefix("v") ?: "1.1.$commitCount"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Minification stays off until release builds are verified on device (F6).
            // proguard-rules.pro is ready for that day (Hilt/Room/Coil/serialization).
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(project(":feature:onboarding:impl"))
    implementation(project(":feature:library:impl"))
    implementation(project(":feature:detail:impl"))
    implementation(project(":feature:reader:impl"))
    implementation(project(":feature:settings:impl"))
    implementation(project(":feature:onboarding:api"))
    implementation(project(":feature:detail:api"))
    implementation(project(":feature:reader:api"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
    implementation(project(":comic-core"))
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material3.windowsize)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":core:test-fakes"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.compose.ui.test.junit4)
}

/**
 * Number of commits reachable from HEAD; drives the incremental dev version so
 * every commit produces a distinct, monotonically increasing versionCode and a
 * `1.1.N` versionName. Returns 0 when git is unavailable (shallow
 * checkouts should use fetch-depth 0; see .github/workflows).
 */
fun gitCommitCount(): Int {
    return try {
        val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        if (process.waitFor() == 0) output.toInt() else 0
    } catch (_: Exception) {
        0
    }
}
