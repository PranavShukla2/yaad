// The Money tab: this month's dashboard, loans, bills, subscriptions, cards, insurance and investments.
plugins {
    alias(libs.plugins.yaad.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.reminders)
            api(projects.feature.loans)
            implementation(libs.compose.ui.backhandler)
        }
    }
}

compose.resources {
    packageOfResClass = "com.pranavshukla.yaad.feature.money.resources"
}
