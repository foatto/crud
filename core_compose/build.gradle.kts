val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

val androidCompileSdk: String by project
val androidTargetSdk: String by project
val androidMinSdk: String by project

val androidxActivityComposeVersion: String by project
val androidxAppcompatVersion: String by project

val coreComposeVersion: String by project

plugins {
    kotlin("multiplatform")

    id("com.android.library")
    id("org.jetbrains.compose")

    `maven-publish`
}

version = coreComposeVersion

kotlin {
    js(IR) {
        browser()
    }

    android()

//    jvm {
//        val jvmMain by compilations.getting {
//            kotlinOptions {
//                languageVersion = kotlinLanguageVersion
//                apiVersion = kotlinApiVersion
//                jvmTarget = kotlinJvmTarget
//                freeCompilerArgs = listOf("-Xjsr305=strict")
//                suppressWarnings = isBuildSupressWarning.toBoolean()
//            }
//        }
//    }
//    jvm("desktop") {
//        compilations.all {
//            kotlinOptions.jvmTarget = "17"
//        }
//    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
//                //--- пока несовместимо с jsMain-compose.html, перенесём в androidMain
//                api(compose.foundation)
//                api(compose.material)
//                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
//                api(compose.components.resources)

                api(project(":core"))
            }
        }
        val jsMain by getting {
            dependencies {
                api(compose.html.core)
                api(compose.html.svg)
            }
        }
        val androidMain by getting {
            dependencies {
                api(compose.foundation)
                api(compose.material)
                api(compose.material3)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                api(compose.components.resources)

                api("androidx.activity:activity-compose:$androidxActivityComposeVersion")
                api("androidx.appcompat:appcompat:$androidxAppcompatVersion")
//                api("androidx.core:core-ktx:1.3.1") - ?

//                ktor-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
//                ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
//                ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
//kotlinx-coroutines = "1.6.4"
//kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
//kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "kotlinx-coroutines" }
//compose-uitooling = "1.4.1"
//compose-uitooling = { module = "androidx.compose.ui:ui-tooling", version.ref = "compose-uitooling" }
            }
        }
//        val jvmMain by getting {
//            dependencies {
//                api(compose.preview)
//            }
//        }
    }
}

android {
    namespace = "foatto.core_compose"

    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/res")
        resources.srcDirs("src/commonMain/resources")
    }

    compileSdk = androidCompileSdk.toInt()
    defaultConfig {
        minSdk = androidMinSdk.toInt()
        targetSdk = androidTargetSdk.toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])  // здесь нет java-кода, прокатывает только с kotlin
        }
    }
    repositories {
        mavenLocal()
//        maven {
//            url = uri("http://.../")
//        }
    }
}
