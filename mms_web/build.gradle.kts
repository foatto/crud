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
    target {
        browser {
        }
    }
}

tasks {
    compileKotlinJs {
        kotlinOptions {
            languageVersion = kotlinLanguageVersion
            apiVersion = kotlinApiVersion
            suppressWarnings = isBuildSupressWarning.toBoolean()
            sourceMap = true
         }
    }
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

