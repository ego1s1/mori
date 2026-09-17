plugins {
    alias(libs.plugins.mori.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.mori.android.compose)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.mori.core.designsystem"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    testImplementation(libs.junit)
}
