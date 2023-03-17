val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

//val jacksonModuleKotlinVersion: String by project
val springBootVersion: String by project

val coreServerMvcVersion: String by project

plugins {
    kotlin("jvm")

    kotlin("plugin.spring")
    kotlin("plugin.jpa")

    id("io.spring.dependency-management")
    id("org.springframework.boot")

    `maven-publish`
}

version = coreServerMvcVersion

dependencies {
    //api("org.springframework.boot:spring-boot-starter:$springBootVersion") - redundantly, come with starter-jdbc or starter-web
    //api("org.springframework.boot:spring-boot-starter-jdbc:$springBootVersion") - redundantly, come with starter-data-jpa
    api("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    api("org.springframework.boot:spring-boot-starter-data-jpa:$springBootVersion")
// compile group: 'com.vladmihalcea', name: 'hibernate-types-52', version: '2.10.0'

    //--- в режиме тестирования пока буду прописывать отдельные зависимости
    //api("org.springframework.boot:spring-boot-starter-jooq:$springBootVersion")

//!!! не должно, но работает :)
//    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonModuleKotlinVersion")
    //implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.11.2")

    api(project(":core_server"))

//    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
}

allOpen {
    annotation("javax.persistence.Entity")
    annotation("javax.persistence.MappedSuperclass")
    annotation("javax.persistence.Embeddable")
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
    bootJar {
        enabled = false
        mainClass.set("foatto.ds.DataServer")  // implicit select one main class from others
    }
    jar {
        enabled = true
    }
}

