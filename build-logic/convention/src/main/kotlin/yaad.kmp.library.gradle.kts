// A Kotlin Multiplatform library for every Yaad target: Android, iOS (device and Apple-silicon simulator) and the
// JVM `desktop` target used for fast tests and previews. `core:*` modules apply only this; UI modules add
// `yaad.kmp.compose`.
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    jvmToolchain(libs.int("jvm-toolchain"))

    android {
        namespace = yaadNamespace
        compileSdk = libs.int("android-compileSdk")
        minSdk = libs.int("android-minSdk")
    }
    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
