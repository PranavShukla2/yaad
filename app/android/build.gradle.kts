plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.pranavshukla.yaad.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.pranavshukla.yaad"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    lint {
        warningsAsErrors = true
        // Version nags would turn CI red whenever a dependency releases; updates are done deliberately instead.
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
    }

    androidResources {
        // English now, Hindi in Phase 10; dependencies bring many more locales.
        localeFilters += listOf("en", "hi")
    }
}

kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
}

dependencies {
    implementation(projects.app.shared)
    implementation(projects.data)
    implementation(projects.platform.notifications)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
}
