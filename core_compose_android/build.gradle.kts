val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

val androidCompileSdk: String by project
val androidTargetSdk: String by project
val androidMinSdk: String by project

plugins {
    kotlin("multiplatform")

    id("com.android.application")
    id("org.jetbrains.compose")
}

kotlin {
//    targetHierarchy.default() - ?

    androidTarget()

    sourceSets {
        val androidMain by getting {
            dependencies {
                api(project(":core_compose"))
            }
        }
    }
}

android {
    namespace = "foatto.core_compose_android"

    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
    }
//    sourceSets["main"].res.srcDirs("src/androidMain/res")
//    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    compileSdk = androidCompileSdk.toInt()
    defaultConfig {
        applicationId = "foatto.core_compose_android"
        minSdk = androidMinSdk.toInt()
        targetSdk = androidTargetSdk.toInt()
        versionCode = 1
        versionName = "1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
//    packagingOptions {
//        resources.excludes.add("META-INF/**")
//    }
}
