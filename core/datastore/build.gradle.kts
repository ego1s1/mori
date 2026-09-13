plugins {
    alias(libs.plugins.mori.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.mori.hilt)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.mori.core.datastore"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.bundles.test.common)
    testImplementation(libs.turbine)
    testImplementation(project(":core:testing"))
}
