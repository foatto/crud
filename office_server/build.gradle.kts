val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

plugins {
    kotlin("jvm")

    kotlin("plugin.spring")
    kotlin("plugin.jpa")

    id("io.spring.dependency-management")
    id("org.springframework.boot")

    idea
}

dependencies {
    api(project(":core_server"))
    api(project(":office_core"))
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
            delete("/home/foatto/OfficeServerSpring/lib")
            copy {
                from("build/libs/office_server.jar", configurations["runtimeClasspath"])
                into("/home/foatto/OfficeServerSpring/lib")
            }
        }
    }
}
