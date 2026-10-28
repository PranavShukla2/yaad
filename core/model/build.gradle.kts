// Entities and value types shared by every module: reminders, schedules, recurrence rules and money.
plugins {
    alias(libs.plugins.yaad.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.datetime)
        }
    }
}
