val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

//val javaxMailApiVersion: String by project
val jExcelApiVersion: String by project
//val ktorVersion: String by project
val minioVersion: String by project
val postgresJdbcVersion: String by project
val springBootVersion: String by project
//val zxingVersion: String by project

plugins {
    kotlin("jvm")

    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    kotlin("plugin.serialization")

    id("io.spring.dependency-management")
    id("org.springframework.boot")
}

dependencies {
    runtimeOnly("org.postgresql:postgresql:$postgresJdbcVersion")

    api("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    api("org.springframework.boot:spring-boot-starter-data-jpa:$springBootVersion")

//--- пока не используется
//    api("com.sun.mail:javax.mail:$javaxMailApiVersion")
//    api("javax.mail:javax.mail-api:$javaxMailApiVersion")
    api("net.sourceforge.jexcelapi:jxl:$jExcelApiVersion")

//--- уже не будет использоваться, т.к. репликация будет проихводиться силами самого postgresql
//    api("io.ktor:ktor-client-apache-jvm:$ktorVersion")
//    api("io.ktor:ktor-client-logging-jvm:$ktorVersion")
////    api("io.ktor:ktor-client-auth-jvm:$ktorVersion")
//    api("io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion")
////    api("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion") - later instead jackson
//    api("io.ktor:ktor-serialization-jackson-jvm:$ktorVersion")

    api("io.minio:minio:$minioVersion")

//--- пока не используется
//    api("com.google.zxing:javase:$zxingVersion")

    api(project(":common"))

//    testImplementation("org.testcontainers:testcontainers:$testContainersVer")
//    testImplementation("org.testcontainers:postgresql:$testContainersVer")
//    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
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
    bootJar {
        enabled = false
        mainClass.set("foatto.ds.DataServer")  // implicit select one main class from others
    }
    jar {
        enabled = true
    }
}

