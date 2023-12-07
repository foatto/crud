import org.jetbrains.compose.ExperimentalComposeLibrary

val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

val composePluginVersion: String by project
val composeVersion: String by project

val androidCompileSdk: String by project
val androidTargetSdk: String by project
val androidMinSdk: String by project

val androidxActivityComposeVersion: String by project

plugins {
    kotlin("multiplatform")

    id("com.android.library")
    id("org.jetbrains.compose")
}

kotlin {
//    js(IR) {
//        browser()
//    }

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = kotlinJvmTarget
            }
        }
    }

//    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material)
//            api(compose.material3)
            @OptIn(ExperimentalComposeLibrary::class)
            api(compose.components.resources)

            api(project(":core"))
        }
        androidMain.dependencies {
            api("androidx.compose.ui:ui:$composeVersion")
            api("androidx.compose.ui:ui-tooling-preview:$composeVersion")
            api("androidx.activity:activity-compose:$androidxActivityComposeVersion")

//            api("androidx.appcompat:appcompat:1.6.1")
        }
//        desktopMain.dependencies {
//            implementation(compose.desktop.currentOs)
//        }
    }
}

android {
    namespace = "foatto.core_compose"

    dependencies {
        debugApi("androidx.compose.ui:ui-tooling:$composeVersion")
    }

    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/res")
        resources.srcDirs("src/commonMain/resources")
    }

    compileSdk = androidCompileSdk.toInt()
    defaultConfig {
//        applicationId = "foatto.core_compose"
        minSdk = androidMinSdk.toInt()
//        targetSdk = androidTargetSdk.toInt()
//        versionCode = 1
//        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }

    composeOptions {
        kotlinCompilerExtensionVersion = composeVersion
    }

    buildFeatures {
        compose = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

//compose.desktop {
//    application {
//        mainClass = "MainKt"
//
//        nativeDistributions {
//            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
//            packageName = "org.example.project"
//            packageVersion = "1.0.0"
//        }
//    }
//}
