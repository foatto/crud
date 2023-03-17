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
                api(project(":ts_core"))
            }
        }
    }
}

tasks {
    build {
        doLast {
            delete("/home/foatto/TSServerSpring/web/compose")
            copy {
                from("/home/foatto/crud/ts_compose_web/build/distributions")
                into("/home/foatto/TSServerSpring/web/compose")
            }
        }
    }
}
