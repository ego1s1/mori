plugins {
    alias(libs.plugins.mori.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.mori.android.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.mori.hilt)
}

android {
    namespace = "com.mori.app"

    defaultConfig {
        applicationId = "com.mori.reader"
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":feature:onboarding:impl"))
    implementation(project(":feature:library:impl"))
    implementation(project(":feature:detail:impl"))
    implementation(project(":feature:reader:impl"))
    implementation(project(":feature:onboarding:api"))
    implementation(project(":feature:library:api"))
    implementation(project(":feature:detail:api"))
    implementation(project(":feature:reader:api"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material3.windowsize)
    implementation(libs.kotlinx.coroutines.android)
}
