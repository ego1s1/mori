plugins {
    alias(libs.plugins.mori.android.library)
    alias(libs.plugins.kotlin.serialization)
}

// Unique group: leaf names ("api"/"impl") repeat across features, and Gradle
// derives a project capability from group:name. Shared group+name collapses
// distinct projects into one conflict-resolved candidate.
group = "com.mori.feature.detail.api"

android {
    namespace = "com.mori.feature.detail.api"
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation.compose)
}
