val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project

val coreComposeWebVersion: String by project

plugins {
    kotlin("multiplatform")

//    kotlin("plugin.serialization")

    id("org.jetbrains.compose")

    `maven-publish`
}

version = coreComposeWebVersion

kotlin {
    js(IR) {
        browser()
//        browser {
//            testTask {
//                testLogging.showStandardStreams = true
//                useKarma {
//                    useChromeHeadless()
//                    useFirefox()
//                }
//            }
//        }
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                api(project(":core_compose"))
            }
        }
//        val jsTest by getting {
//            dependencies {
//                implementation(kotlin("test-js"))
//            }
//        }
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
