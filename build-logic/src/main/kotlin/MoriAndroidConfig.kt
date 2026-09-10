import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal const val COMPILE_SDK = 35
internal const val MIN_SDK = 24
internal const val TARGET_SDK = 35

internal fun Project.configureAndroidCommon(
    extension: CommonExtension<*, *, *, *, *, *>,
) {
    extension.apply {
        compileSdk = COMPILE_SDK

        defaultConfig {
            minSdk = MIN_SDK
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

internal fun Project.configureAndroidLibraryDefaults() {
    extensions.configure<LibraryExtension> {
        configureAndroidCommon(this)
        defaultConfig {
            targetSdk = TARGET_SDK
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        testOptions {
            unitTests.isIncludeAndroidResources = true
        }
    }
    disableReleaseUnitTests()
}

internal fun Project.configureAndroidApplicationDefaults() {
    extensions.configure<ApplicationExtension> {
        configureAndroidCommon(this)
        defaultConfig {
            targetSdk = TARGET_SDK
        }
        testOptions {
            unitTests.isIncludeAndroidResources = true
        }
    }
    disableReleaseUnitTests()
}

/**
 * Debug and release unit tests compile and run the same `src/test` sources (unit tests are
 * never minified), so the release variant run is redundant. It is disabled so `./gradlew
 * test` stays green — notably, `debugImplementation`-scoped test manifests (Compose UI
 * test activity) are invisible to the release variant's Robolectric manifest resolution.
 */
internal fun Project.disableReleaseUnitTests() {
    tasks.configureEach {
        if (name.endsWith("ReleaseUnitTest")) {
            enabled = false
        }
    }
}
