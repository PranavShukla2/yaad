// Loans and EMIs: the loan list, editor and detail with its schedule, prepayments, rate changes and foreclosure.
plugins {
    alias(libs.plugins.yaad.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.ui.designsystem)
            api(projects.core.finance)
            api(projects.core.scheduling)
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
    packageOfResClass = "com.pranavshukla.yaad.feature.loans.resources"
}
