val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

val coreComposeVersion: String by project

plugins {
    kotlin("multiplatform")

    id("org.jetbrains.compose")
//    id("com.android.library")

    `maven-publish`
}

version = coreComposeVersion

kotlin {
    js(IR) {
        browser()
    }
//    android()
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
//            kotlinOptions.jvmTarget = "11"
//        }
//    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                //--- появятся вместе с id("com.android.library")
//                api(compose.foundation)
//                api(compose.material)

//                api(project(":core")) - пока непобедимая ошибка
            }
        }
//        val commonTest by getting {
//            dependencies {
//                implementation(kotlin("test"))
//            }
//        }
        val jsMain by getting {
            dependencies {
                api(compose.web.core)
////!!! will need in future
////    api(compose.web.svg)

//                api(project(":core")) - пока непобедимая ошибка
            }
        }
//        val jsTest by getting {
//            dependencies {
//                implementation(kotlin("test-js"))
//            }
//        }
//        val androidMain by getting {
//            dependencies {
//                api("androidx.appcompat:appcompat:1.2.0")
//                api("androidx.core:core-ktx:1.3.1")
//            }
//        }
//        val androidTest by getting {
//            dependencies {
//                implementation("junit:junit:4.13")
//            }
//        }
//        val jvmMain by getting {
//            dependencies {
//                api(compose.preview)
//            }
//        }
    }
}

//android {
//    compileSdkVersion(31)
//    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
//    defaultConfig {
//        minSdkVersion(24)
//        targetSdkVersion(31)
//    }
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_1_8
//        targetCompatibility = JavaVersion.VERSION_1_8
//    }
//}

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

