import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project

plugins {
    kotlin("js")
}

dependencies {
    api(project(":core_web"))
    api(project(":shop_core"))
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
            delete("/home/foatto/ShopServerSpring/web/lib")
            copy {
                from(zipTree("build/libs/shop_web.jar"))
                into("/home/foatto/ShopServerSpring/web/lib")
            }
            configurations["runtimeClasspath"].forEach {
                copy {
                    from(zipTree(it))
                    into("/home/foatto/ShopServerSpring/web/lib")
                }
            }
        }
    }
}

