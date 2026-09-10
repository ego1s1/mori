plugins {
    alias(libs.plugins.mori.jvm.library)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
