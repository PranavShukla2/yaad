// Today, Upcoming, the reminder editor and detail: the screens of Phase 5.
plugins {
    alias(libs.plugins.yaad.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.ui.designsystem)
            api(projects.core.scheduling)
            implementation(projects.core.finance)
            api(projects.core.quickadd)
            api(projects.data)
            implementation(libs.compose.ui.backhandler)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
        named("desktopTest").dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }
    }
}

compose.resources {
    packageOfResClass = "com.pranavshukla.yaad.feature.reminders.resources"
}
