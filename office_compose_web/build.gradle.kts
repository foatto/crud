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
                api(project(":office_core"))
            }
        }
    }
}

tasks {
    build {
        doLast {
            delete("/home/foatto/OfficeServerSpring/web/compose")
            copy {
                from("build/dist/js/productionExecutable")
                into("/home/foatto/OfficeServerSpring/web/compose")
            }
        }
    }
}
