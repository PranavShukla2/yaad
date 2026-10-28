// Theme, components and adaptive layouts shared by every feature module.
plugins {
    alias(libs.plugins.yaad.kmp.compose)
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.pranavshukla.yaad.ui.designsystem.resources"
}
