pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "mori"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":comic-core")
include(":app")
include(":core:model")
include(":core:common")
include(":core:testing")
include(":core:test-fakes")
include(":core:designsystem")
include(":core:datastore")
include(":core:database")
include(":core:data")
include(":feature:onboarding:api")
include(":feature:onboarding:impl")
include(":feature:library:impl")
include(":feature:detail:api")
include(":feature:detail:impl")
include(":feature:reader:api")
include(":feature:reader:impl")
include(":feature:settings:impl")
