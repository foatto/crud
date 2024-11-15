import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

val composeCompilerVersion: String by project
val composeRuntimeVersion: String by project

val androidCompileSdk: String by project
val androidTargetSdk: String by project
val androidMinSdk: String by project

plugins {
    kotlin("multiplatform")

    id("com.android.application")
    id("org.jetbrains.compose")
//    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "compose_ts"
        browser {}
        binaries.executable()
    }

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = kotlinJvmTarget
            }
        }
    }

    jvm {
        val main by compilations.getting {
            kotlinOptions {
                languageVersion = kotlinLanguageVersion
                apiVersion = kotlinApiVersion
                jvmTarget = kotlinJvmTarget
                freeCompilerArgs = listOf("-Xjsr305=strict")
                suppressWarnings = isBuildSupressWarning.toBoolean()
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":compose"))
            api(project(":common_ts"))
        }
    }
}

android {
    namespace = "foatto.compose_ts"

    dependencies {
        debugApi("androidx.compose.ui:ui-tooling:$composeRuntimeVersion")
    }

    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/res")
        resources.srcDirs("src/commonMain/resources")
    }

    compileSdk = androidCompileSdk.toInt()
    defaultConfig {
        applicationId = "foatto.compose_ts"
        minSdk = androidMinSdk.toInt()
        targetSdk = androidTargetSdk.toInt()
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }

    composeOptions {
        kotlinCompilerExtensionVersion = composeCompilerVersion
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
//        mainClass = "foatto.compose_ts.MainKt"
//
//        nativeDistributions {
//            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
//            packageName = "foatto.compose_ts"
//            packageVersion = "1.0.0"
//        }
//    }
//}

//tasks {
//    build {
//        doLast {
//            copy {
//                from("build/dist/wasmJs/productionExecutable")
//                into("/home/foatto/TSServerSpring/web")
//            }
//
//            delete("/home/foatto/TSDesktop/lib")
//            copy {
//                from("build/compose/binaries/main/app/XXX/lib/app")
//                into("/home/foatto/TSDesktop/lib")
//            }
//
///* native distributable
//            delete("/home/foatto/TSDesktop/bin")
//            delete("/home/foatto/TSDesktop/lib")
//            copy {
//                from("build/compose/binaries/main/app/XXX")
//                into("/home/foatto/TSDesktop")
//            }
//*/
//        }
//    }
//}
