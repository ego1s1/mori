plugins {
    alias(libs.plugins.mori.android.library)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.mori.core.database"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
