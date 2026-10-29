// Theme, components and adaptive layouts shared by every feature module.
plugins {
    alias(libs.plugins.yaad.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.datetime)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.pranavshukla.yaad.ui.designsystem.resources"
}
