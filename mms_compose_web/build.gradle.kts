val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project

plugins {
    kotlin("multiplatform")

    kotlin("plugin.serialization")

    id("org.jetbrains.compose")
}

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
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                api(project(":core_compose_web"))
//    api(project(":mms_core"))
            }
        }
//        val jsTest by getting {
//            dependencies {
//                implementation(kotlin("test-js"))
//            }
//        }
    }
}

tasks {
    build {
        doLast {
            delete("/home/foatto/MMSServerSpring/web/compose")
            copy {
                from("/home/foatto/crud/mms_compose_web/build/distributions")
                into("/home/foatto/MMSServerSpring/web/compose")
            }
        }
    }
}
