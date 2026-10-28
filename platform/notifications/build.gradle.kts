// Turns the alert plan into real notifications: AlarmManager on Android, UNUserNotificationCenter on iOS.
plugins {
    alias(libs.plugins.yaad.kmp.library)
}

kotlin {
    android {
        androidResources { enable = true }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.scheduling)
            api(projects.data)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core)
        }
    }
}
