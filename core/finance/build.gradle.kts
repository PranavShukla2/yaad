// EMI, amortisation, prepayment and rate changes, foreclosure, and money formatting: exact, never floating point.
plugins {
    alias(libs.plugins.yaad.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            implementation(libs.bignum)
        }
    }
}
