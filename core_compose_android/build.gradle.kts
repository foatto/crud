val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

val androidCompileSdk: String by project
val androidTargetSdk: String by project
val androidMinSdk: String by project

val coreComposeAndroidVersion: String by project

plugins {
    kotlin("multiplatform")

    id("com.android.application")
    id("org.jetbrains.compose")

    `maven-publish`
}

version = coreComposeAndroidVersion

kotlin {
//    targetHierarchy.default() - ?

    android()

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
        versionName = coreComposeAndroidVersion
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        jvmToolchain(11)
    }
//    packagingOptions {
//        resources.excludes.add("META-INF/**")
//    }
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
