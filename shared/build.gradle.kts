plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    // id("com.android.library")  // Android SDK 필요시 활성화
}

// 버전 관리
val buildNumber = (project.findProperty("build.number") as String?)?.toIntOrNull() ?: 1
val nextBuildNumber = buildNumber + 1

// 빌드 완료 시 자동 증가
gradle.buildFinished {
    project.file("gradle.properties").apply {
        if (exists()) {
            val props = readLines().map { line ->
                if (line.startsWith("build.number=")) {
                    "build.number=$nextBuildNumber"
                } else {
                    line
                }
            }
            writeText(props.joinToString("\n"))
        } else {
            writeText("build.number=$nextBuildNumber\n")
        }
    }
}

// Version.kt 생성
task("generateVersion") {
    doLast {
        val versionFile = file("src/commonMain/kotlin/com/bomcomes/calculator/Version.kt")
        versionFile.writeText("""
package com.bomcomes.calculator

/**
 * Library version information
 * This file is auto-generated during build
 */
object Version {
    const val VERSION = "1.0.0"
    const val BUILD_NUMBER = $buildNumber

    fun getVersionString(): String = "period-calculator-kmp v${"$"}VERSION (build ${"$"}BUILD_NUMBER)"
}
        """.trimIndent())
    }
}

afterEvaluate {
    tasks.findByName("compileKotlinJvm")?.dependsOn("generateVersion")
    tasks.findByName("compileKotlinJs")?.dependsOn("generateVersion")
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

    // JVM 타겟 (테스트 및 디버깅용)
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        // val androidMain by getting
        // val iosMain by creating
        val jvmMain by getting
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
