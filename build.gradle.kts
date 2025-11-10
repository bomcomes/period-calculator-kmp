plugins {
    kotlin("multiplatform") version "1.9.22" apply false
    kotlin("plugin.serialization") version "1.9.22" apply false
    // id("com.android.library") version "8.1.0" apply false  // Android SDK 필요시 활성화
}

allprojects {
    repositories {
        mavenCentral()
    }
}
