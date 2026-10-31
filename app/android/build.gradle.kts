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
        // CI passes these for releases (see .github/workflows/release.yml).
        versionCode = (findProperty("yaad.versionCode") as String?)?.toInt() ?: 1
        versionName = findProperty("yaad.versionName") as String? ?: "0.1.0"
    }

    // The release key never lives in the repository: CI decodes it from encrypted secrets into these variables.
    val keystore = System.getenv("YAAD_KEYSTORE")
    signingConfigs {
        if (keystore != null) {
            create("release") {
                storeFile = file(keystore)
                storePassword = System.getenv("YAAD_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("YAAD_KEY_ALIAS")
                keyPassword = System.getenv("YAAD_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystore != null) signingConfig = signingConfigs.getByName("release")
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
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment)
}
