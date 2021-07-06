val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

val jacksonModuleKotlinVersion: String by project
val javaxMailApiVersion: String by project
val jExcelApiVersion: String by project
val ktorVersion: String by project
val postgresJdbcVersion: String by project
val springBootVersion: String by project

val coreServerVersion: String by project

plugins {
    kotlin("jvm")

    kotlin("plugin.spring")
    kotlin("plugin.jpa")

    id("io.spring.dependency-management")
    id("org.springframework.boot")

    `maven-publish`
    idea
}

version=coreServerVersion

dependencies {
    //api("org.springframework.boot:spring-boot-starter:$springBootVersion") - redundantly, come with starter-jdbc or starter-web
    //api("org.springframework.boot:spring-boot-starter-jdbc:$springBootVersion") - redundantly, come with starter-data-jpa
    api("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    api("org.springframework.boot:spring-boot-starter-data-jpa:$springBootVersion")
// compile group: 'com.vladmihalcea', name: 'hibernate-types-52', version: '2.10.0'

    runtimeOnly("org.postgresql:postgresql:$postgresJdbcVersion")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonModuleKotlinVersion")

    api("javax.mail:javax.mail-api:$javaxMailApiVersion")
    api("net.sourceforge.jexcelapi:jxl:$jExcelApiVersion")

    api("io.ktor:ktor-client-apache:$ktorVersion")
    api("io.ktor:ktor-client-jackson:$ktorVersion")
    api("io.ktor:ktor-client-logging-jvm:$ktorVersion")

//    api("io.ktor:ktor-client-auth-jvm:$ktorVersion")

    api(project(":core"))

//    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
//    testImplementation("org.testcontainers:testcontainers:$testContainersVer")
//    testImplementation("org.testcontainers:postgresql:$testContainersVer")
//    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
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
        mainClassName = "foatto.ds.DataServer"  // implicit select one main class from others
    }
    jar {
        enabled = true
    }
}
