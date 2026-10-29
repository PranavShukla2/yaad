// Quick add: "rent 15000 on 5th every month" into a reminder, in English and Hinglish.
plugins {
    alias(libs.plugins.yaad.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
        }
    }
}
