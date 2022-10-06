val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

val mmsServerVersion: String by project

plugins {
    kotlin("jvm")

    `maven-publish`
}

version = mmsServerVersion

dependencies {
    api(project(":core_server"))
    api(project(":mms_core"))
}

tasks {
    compileKotlin {
        kotlinOptions {
            languageVersion = kotlinLanguageVersion
            apiVersion = kotlinApiVersion
            jvmTarget = kotlinJvmTarget
            freeCompilerArgs = listOf("-Xjsr305=strict")
            suppressWarnings = isBuildSupressWarning.toBoolean()
        }
    }
    jar {
        enabled = true
    }
}
