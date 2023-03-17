val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

val kotlinxSerializationVersion: String by project

val coreVersion: String by project

plugins {
    kotlin("multiplatform")

    kotlin("plugin.serialization")

    `maven-publish`
}

version = coreVersion

kotlin {
    js(IR) {  // IR-backend version
        browser {
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
        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
            }
        }
        val jsMain by getting {
        }
        val jvmMain by getting {
            dependencies {
                api(kotlin("reflect"))
            }
        }
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

