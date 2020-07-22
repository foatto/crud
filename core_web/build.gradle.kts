val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project

val coreWebVersion: String by project

plugins {
    kotlin("js")

    `maven-publish`
    idea
}

version=coreWebVersion

dependencies {
    api(kotlin("stdlib-js"))
    api(project(":core"))
}

kotlin {
    target {
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
////        maven {
////            url = uri("http://nexus.otr.ru/content/repositories/core-release/")
////        }
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
}

