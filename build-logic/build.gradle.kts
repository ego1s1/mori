plugins {
    `kotlin-dsl`
}

group = "com.mori.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "mori.android.application"
            implementationClass = "MoriAndroidApplicationPlugin"
        }
        register("androidLibrary") {
            id = "mori.android.library"
            implementationClass = "MoriAndroidLibraryPlugin"
        }
        register("androidFeature") {
            id = "mori.android.feature"
            implementationClass = "MoriAndroidFeaturePlugin"
        }
        register("androidCompose") {
            id = "mori.android.compose"
            implementationClass = "MoriAndroidComposePlugin"
        }
        register("hilt") {
            id = "mori.hilt"
            implementationClass = "MoriHiltPlugin"
        }
        register("jvmLibrary") {
            id = "mori.jvm.library"
            implementationClass = "MoriJvmLibraryPlugin"
        }
    }
}
