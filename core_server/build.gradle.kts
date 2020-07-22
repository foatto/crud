val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

val jacksonModuleKotlinVersion: String by project
val javaxMailApiVersion: String by project
val jExcelApiVersion: String by project
val postgresJdbcVersion: String by project
val springBootVersion: String by project

val coreServerVersion: String by project

plugins {
    kotlin("jvm")

//    kotlin("plugin.allopen")
//    kotlin("plugin.jpa")
    kotlin("plugin.spring")

    id("io.spring.dependency-management")
    id("org.springframework.boot")

    `maven-publish`
    idea
}

version=coreServerVersion

dependencies {
    api("org.springframework.boot:spring-boot-starter:$springBootVersion")
    api("org.springframework.boot:spring-boot-starter-jdbc:$springBootVersion")
//    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-web:$springBootVersion")

    runtimeOnly("org.postgresql:postgresql:$postgresJdbcVersion")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonModuleKotlinVersion")

    api("javax.mail:javax.mail-api:$javaxMailApiVersion")
    api("net.sourceforge.jexcelapi:jxl:$jExcelApiVersion")

    api(project(":core"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])    // можно написать "kotlin", но тогда будут не совсем точные описатели в module и pom
        }
    }
    repositories {
        mavenLocal()
//        maven {
//            url = uri("http://.../")
//        }
    }
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
    bootJar {
        enabled = false
    }
}
