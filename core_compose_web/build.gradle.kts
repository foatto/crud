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
        jsMain.dependencies {
            api(compose.html.core)
            api(compose.html.svg)
            api(project(":core"))
        }
    }
}

