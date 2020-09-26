import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project

plugins {
    kotlin("js")
    idea
}

dependencies {
    api(project(":core_web"))
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
            delete("/home/foatto/MMSServerSpring/web/lib")
            copy {
                from(zipTree("build/libs/mms_web.jar"))
                into("/home/foatto/MMSServerSpring/web/lib")
            }
            configurations["runtimeClasspath"].forEach {
                copy {
                    from(zipTree(it))
                    into("/home/foatto/MMSServerSpring/web/lib")
                }
            }
        }
    }
}

