import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

val composePluginVersion: String by project
val composeVersion: String by project
val composeCompilerVersion: String by project

val androidCompileSdk: String by project
val androidTargetSdk: String by project
val androidMinSdk: String by project

val kotlinxCoroutinesVersion: String by project
val ktorVersion: String by project

val multiplatformSettingsVersion: String by project
val kamelImageVersion: String by project

val androidxActivityComposeVersion: String by project

plugins {
    kotlin("multiplatform")

    id("com.android.library")
    id("org.jetbrains.compose")
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

    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")

//WASM!!! пока не готов релиз wasm-версии ktor-client - сделаем отдельные зависимости в android-части,
// а у wasm будем использовать "родной" XMLHttpRequest
//
//            //api("io.ktor:ktor-client-core:$ktorVersion") - уже есть в ktor-client-content-negotiation
//            api("io.ktor:ktor-client-content-negotiation:$ktorVersion")
//            api("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

            api(compose.runtime)
            api(compose.foundation)
            api(compose.ui)
            api(compose.material3)

//            api(compose.components.resources) - пока не надо
//            api(compose.components.uiToolingPreview)

            api("com.russhwolf:multiplatform-settings:$multiplatformSettingsVersion")
            api("com.russhwolf:multiplatform-settings-no-arg:$multiplatformSettingsVersion")
            api("media.kamel:kamel-image-default:$kamelImageVersion")

            api(project(":core"))
        }
        androidMain.dependencies {
            api("org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinxCoroutinesVersion")
            api("io.ktor:ktor-client-android:$ktorVersion")
//            api("io.ktor:ktor-client-okhttp:$ktorVersion") - alternate engine for http/2 & WebSockets support

            api("androidx.compose.ui:ui-tooling-preview:$composeVersion")
            api("androidx.activity:activity-compose:$androidxActivityComposeVersion")

//WASM!!! пока не готов релиз wasm-версии ktor-client - сделаем отдельные зависимости в android-части,
// а у wasm будем использовать "родной" XMLHttpRequest
            //api("io.ktor:ktor-client-core-jvm:$ktorVersion") - уже есть в ktor-client-content-negotiation
            api("io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion")
            api("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
        }
    }
}
// ktor-client-js:3.0.0-wasm2

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
        minSdk = androidMinSdk.toInt()
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

compose.experimental {
    web.application {}
}