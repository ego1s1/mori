plugins {
    alias(libs.plugins.mori.jvm.library)
    alias(libs.plugins.detekt)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    implementation(libs.junit)
}
