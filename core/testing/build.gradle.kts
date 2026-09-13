plugins {
    alias(libs.plugins.mori.jvm.library)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    implementation(libs.junit)
}
