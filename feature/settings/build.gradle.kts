// Settings: alert times, quiet hours, clock, currency, theme, privacy and backup.
plugins {
    alias(libs.plugins.yaad.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.ui.designsystem)
            api(projects.data)
        }
    }
}

compose.resources {
    packageOfResClass = "com.pranavshukla.yaad.feature.settings.resources"
}
