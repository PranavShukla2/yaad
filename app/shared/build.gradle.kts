import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

// The whole app as one multiplatform module: Android and desktop depend on it, iOS links it as a framework.
plugins {
    alias(libs.plugins.yaad.kmp.compose)
}

kotlin {
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "YaadShared"
            isStatic = true
            // SQLDelight's native driver uses the system SQLite.
            linkerOpts("-lsqlite3")
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.ui.designsystem)
            api(projects.feature.reminders)
            api(projects.feature.loans)
            api(projects.feature.money)
            api(projects.feature.settings)
        }
        named("desktopTest").dependencies {
            implementation(libs.sqldelight.sqlite.driver)
            implementation(projects.platform.notifications)
        }
        iosMain.dependencies {
            implementation(projects.data)
            implementation(projects.platform.notifications)
        }
    }
}

compose.resources {
    packageOfResClass = "com.pranavshukla.yaad.app.resources"
}
