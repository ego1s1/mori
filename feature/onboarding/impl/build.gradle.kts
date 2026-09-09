plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.mori.android.feature)
}

// Unique group: leaf names ("api"/"impl") repeat across features, and Gradle
// derives a project capability from group:name. Shared group+name collapses
// distinct projects into one conflict-resolved candidate.
group = "com.mori.feature.onboarding.impl"

android {
    namespace = "com.mori.feature.onboarding.impl"
}

dependencies {
    api(project(":feature:onboarding:api"))
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:datastore"))
}
