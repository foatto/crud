import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJsDce

val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project

plugins {
    kotlin("js")
}

dependencies {
    api(project(":core_web"))
    api(project(":mms_core"))
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

tasks.withType<KotlinJsDce>().configureEach {
    jvmArgs.add("-Xss2m")
}

tasks {
    build {
        doLast {
            delete("/home/foatto/MMSServerSpring/web/lib")
            //--- LEGACY-backend version
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
            //--- IR-backend version
//            copy {
//                from("build/distributions")
//                into("/home/foatto/MMSServerSpring/web/lib")
//            }
//            copy {
//                from("build/compileSync/main/productionExecutable/kotlin")
//                into("/home/foatto/MMSServerSpring/web/lib")
//            }
        }
    }
}

