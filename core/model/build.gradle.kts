plugins {
    alias(libs.plugins.mori.jvm.library)
    alias(libs.plugins.detekt)
}

dependencies {
    testImplementation(libs.junit)
}
