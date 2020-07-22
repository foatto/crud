val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

val coreVersion: String by project

plugins {
    kotlin("multiplatform") 

    `maven-publish`
    idea
}

version=coreVersion

kotlin {
    js {
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
                api(kotlin("stdlib-common"))
            }
        }
        val jsMain by getting {
            dependencies {
                api(kotlin("stdlib-js"))
            }
        }
        val jvmMain by getting {
            dependencies {
                api(kotlin("stdlib-jdk8"))
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
////        maven {
////            url = uri("http://nexus.otr.ru/content/repositories/core-release/")
////        }
    }
}

