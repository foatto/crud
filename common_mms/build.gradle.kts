import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

val kotlinxSerializationVersion: String by project
val kotlinxDatetimeVersion: String by project

val androidCompileSdk: String by project
val androidMinSdk: String by project

plugins {
    kotlin("multiplatform")

    kotlin("plugin.serialization")
    id("com.android.library")
}

kotlin {

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
        }
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
            api(project(":common"))
        }
    }
}

android {
    namespace = "foatto.core_mms"

    compileSdk = androidCompileSdk.toInt()
    defaultConfig {
        minSdk = androidMinSdk.toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
