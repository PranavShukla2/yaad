// The next-occurrence engine: pure Kotlin, exhaustively tested.
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
