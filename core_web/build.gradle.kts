import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJsDce

val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project

val coreWebVersion: String by project

plugins {
    kotlin("js")

    `maven-publish`
}

version = coreWebVersion

dependencies {
    api(project(":core"))
}

kotlin {
    js {// LEGACY-backend version
//    js(IR) {  // IR-backend version
        browser {
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])  // здесь нет java-кода, прокатывает только с kotlin
        }
    }
    repositories {
        mavenLocal()
//        maven {
//            url = uri("http://.../")
//        }
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
