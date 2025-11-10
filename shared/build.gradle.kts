plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    // id("com.android.library")  // Android SDK 필요시 활성화
}

kotlin {
    // Android 타겟 (Android SDK 필요)
    // androidTarget {
    //     publishLibraryVariants("release", "debug")
    // }

    // iOS 타겟 (macOS + Xcode 필요)
    // listOf(
    //     iosX64(),
    //     iosArm64(),
    //     iosSimulatorArm64()
    // ).forEach {
    //     it.binaries.framework {
    //         baseName = "PeriodCalculator"
    //         isStatic = true
    //     }
    // }

    // JavaScript/Node.js 타겟 (SDK 불필요)
    js(IR) {
        nodejs()
        binaries.library()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        // val androidMain by getting
        // val iosMain by creating
        val jsMain by getting

        // val iosX64Main by getting
        // val iosArm64Main by getting
        // val iosSimulatorArm64Main by getting

        // iosX64Main.dependsOn(iosMain)
        // iosArm64Main.dependsOn(iosMain)
        // iosSimulatorArm64Main.dependsOn(iosMain)
    }
}

// Android 설정 (Android SDK 필요시 활성화)
// android {
//     namespace = "com.bomcomes.calculator"
//     compileSdk = 34
//
//     defaultConfig {
//         minSdk = 24
//     }
// }
