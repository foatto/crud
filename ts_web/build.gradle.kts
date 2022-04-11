import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project

plugins {
    kotlin("js")
}

dependencies {
    api(project(":core_web"))
    api(project(":ts_core"))
}

kotlin {
    js {
        browser {
        }
    }
}

tasks.withType<Kotlin2JsCompile>().configureEach {
    kotlinOptions {
        languageVersion = kotlinLanguageVersion
        apiVersion = kotlinApiVersion
        suppressWarnings = isBuildSupressWarning.toBoolean()
        sourceMap = true
    }
}

tasks {
    build {
        doLast {
            delete("/home/foatto/TSServerSpring/web/lib")
            copy {
                from(zipTree("build/libs/ts_web.jar"))
                into("/home/foatto/TSServerSpring/web/lib")
            }
            configurations["runtimeClasspath"].forEach {
                copy {
                    from(zipTree(it))
                    into("/home/foatto/TSServerSpring/web/lib")
                }
            }
        }
    }
}

