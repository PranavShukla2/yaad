// Decides which notifications should exist: a pure function of reminders, their history, the time and the zone.
plugins {
    alias(libs.plugins.yaad.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.recurrence)
        }
    }
}
