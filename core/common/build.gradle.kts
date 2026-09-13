plugins {
    alias(libs.plugins.mori.jvm.library)
    alias(libs.plugins.detekt)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
