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
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                api(project(":core_compose"))
            }
        }
    }
}
