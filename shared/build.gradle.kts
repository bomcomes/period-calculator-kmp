plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    // Android 타겟
    androidTarget {
        publishLibraryVariants("release", "debug")
    }
    
    // iOS 타겟
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "PeriodCalculator"
            isStatic = true
        }
    }
    
    // JavaScript/Node.js 타겟
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
        
        val androidMain by getting
        val iosMain by creating
        val jsMain by getting
        
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        
        iosX64Main.dependsOn(iosMain)
        iosArm64Main.dependsOn(iosMain)
        iosSimulatorArm64Main.dependsOn(iosMain)
    }
}

android {
    namespace = "com.bomcomes.calculator"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 24
    }
}
