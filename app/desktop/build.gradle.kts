import org.jetbrains.compose.desktop.application.dsl.TargetFormat

// A development-only desktop window for quick previews; Yaad ships on Android and iOS.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
}

dependencies {
    implementation(projects.app.shared)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "com.pranavshukla.yaad.desktop.MainKt"
        if (System.getProperty("os.name").startsWith("Mac")) jvmArgs += "-Xdock:name=Yaad"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "Yaad"
            packageVersion = "1.0.0"
        }
    }
}
