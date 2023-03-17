val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project

val coreComposeWebVersion: String by project

plugins {
    kotlin("multiplatform")

    id("org.jetbrains.compose")

    `maven-publish`
}

version = coreComposeWebVersion

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
