import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.mori.android.library)
    alias(libs.plugins.detekt)
    alias(libs.plugins.binary.compatibility.validator)
    id("maven-publish")
}

android {
    namespace = "com.mori.comic.core"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.junrar)

    testImplementation(libs.bundles.test.common)
}

detekt {
    buildUponDefaultConfig = true
    source = files("src/main/kotlin")
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.mori"
                artifactId = "comic-core"
                version = project.version.toString()
            }
        }
    }
}
