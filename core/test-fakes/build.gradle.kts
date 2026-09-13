plugins {
    alias(libs.plugins.mori.android.library)
}

android {
    namespace = "com.mori.core.testfakes"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:data"))
    api(project(":core:datastore"))
    api(libs.kotlinx.coroutines.core)
}
