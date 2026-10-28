// Adds Compose Multiplatform (Material 3, resources, UI tests) to a `yaad.kmp.library` module.
plugins {
    id("yaad.kmp.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    android {
        androidResources { enable = true }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.lib("compose-runtime"))
            implementation(libs.lib("compose-foundation"))
            implementation(libs.lib("compose-ui"))
            implementation(libs.lib("compose-material3"))
            implementation(libs.lib("compose-components-resources"))
            implementation(libs.lib("compose-ui-tooling-preview"))
        }
        commonTest.dependencies {
            implementation(libs.lib("compose-ui-test"))
        }
        named("desktopTest").dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}
