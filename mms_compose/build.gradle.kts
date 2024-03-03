import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

val composeVersion: String by project
val composeCompilerVersion: String by project

val androidCompileSdk: String by project
val androidTargetSdk: String by project
val androidMinSdk: String by project

plugins {
    kotlin("multiplatform")

    id("com.android.application")
    id("org.jetbrains.compose")
}

kotlin {

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "mms_compose"
        browser {
//--- только для локального тест-запуска
//            commonWebpackConfig {
//                outputFileName = "mms_compose.js"
//                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
//                    static = (static ?: mutableListOf()).apply {
//                        // Serve sources to debug inside browser
//                        //add(project.projectDir.path)
//                        clear()
//                        add("/home/foatto/crud/mms_compose/build/dist/wasmJs/productionExecutable")
//                    }
//                }
//            }
        }
        binaries.executable()
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
            api(project(":core_compose"))
//            api(project(":mms_core"))
        }
    }
}

android {
    namespace = "foatto.mms_compose"

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
        applicationId = "foatto.mms_compose"
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

compose.experimental {
    web.application {}
}

//tasks {
//    build {
//        doLast {
//            delete("/home/foatto/MMSServerSpring/web/compose")
//            copy {
//                from("build/dist/js/productionExecutable")
//                into("/home/foatto/MMSServerSpring/web/compose")
//            }
//        }
//    }
//}
