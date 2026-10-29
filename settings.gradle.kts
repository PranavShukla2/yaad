rootProject.name = "yaad"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}
include(":ui:designsystem")
include(":app:shared")
include(":app:android")
include(":app:desktop")
include(":core:model")
include(":core:recurrence")
include(":data")
include(":core:scheduling")
include(":platform:notifications")
include(":feature:reminders")
include(":core:finance")
include(":feature:loans")
include(":feature:money")
include(":core:quickadd")
include(":feature:settings")
