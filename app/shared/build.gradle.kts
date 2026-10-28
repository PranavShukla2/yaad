import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

// The whole app as one multiplatform module: Android and desktop depend on it, iOS links it as a framework.
plugins {
    alias(libs.plugins.yaad.kmp.compose)
}

kotlin {
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "YaadShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.ui.designsystem)
        }
    }
}

compose.resources {
    packageOfResClass = "com.pranavshukla.yaad.app.resources"
}
