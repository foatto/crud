val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project

plugins {
    kotlin("multiplatform")

    id("org.jetbrains.compose")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                api(project(":core_compose_web"))
                api(project(":mms_core"))
            }
        }
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
