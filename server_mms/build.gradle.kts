val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

plugins {
    kotlin("jvm")

    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    kotlin("plugin.serialization")

    id("io.spring.dependency-management")
    id("org.springframework.boot")
}

dependencies {
    api(project(":server"))
    api(project(":common_mms"))
}

allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.MappedSuperclass")
    annotation("javax.persistence.Embeddable")
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
    build {
        doLast {
            delete("/home/foatto/MMSServerSpring/lib_2")
            copy {
                from("build/libs/server_mms-plain.jar", configurations["runtimeClasspath"])
                into("/home/foatto/MMSServerSpring/lib_2")
            }
        }
    }
}
