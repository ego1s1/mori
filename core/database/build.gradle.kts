plugins {
    alias(libs.plugins.mori.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.mori.hilt)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.mori.core.database"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.bundles.test.common)
    testImplementation(libs.turbine)
    testImplementation(project(":core:testing"))
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
