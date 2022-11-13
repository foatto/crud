import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project

plugins {
    kotlin("js")
}

dependencies {
    api(project(":core_web"))
    api(project(":office_core"))
}

kotlin {
    js {// LEGACY-backend version
//    js(IR) {  // IR-backend version
        browser {
        }
        binaries.executable()
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
            delete("/home/foatto/OfficeServerSpring/web/lib")
            //--- LEGACY-backend version
            copy {
                from(zipTree("build/libs/office_web.jar"))
                into("/home/foatto/OfficeServerSpring/web/lib")
            }
            configurations["runtimeClasspath"].forEach {
                copy {
                    from(zipTree(it))
                    into("/home/foatto/OfficeServerSpring/web/lib")
                }
            }
            //--- IR-backend version
//            copy {
//                from("build/distributions")
//                into("/home/foatto/OfficeServerSpring/web/lib")
//            }
//            copy {
//                from("build/compileSync/main/productionExecutable/kotlin")
//                into("/home/foatto/OfficeServerSpring/web/lib")
//            }
        }
    }
}

