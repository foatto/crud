val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

val javaxMailApiVersion: String by project
val jExcelApiVersion: String by project
val ktorVersion: String by project
val minioVersion: String by project
val postgresJdbcVersion: String by project

plugins {
    kotlin("jvm")
}

dependencies {
    runtimeOnly("org.postgresql:postgresql:$postgresJdbcVersion")

    api("com.sun.mail:javax.mail:$javaxMailApiVersion")
    api("javax.mail:javax.mail-api:$javaxMailApiVersion")
    api("net.sourceforge.jexcelapi:jxl:$jExcelApiVersion")

    api("io.ktor:ktor-client-apache:$ktorVersion")
    api("io.ktor:ktor-client-jackson:$ktorVersion")
    api("io.ktor:ktor-client-logging-jvm:$ktorVersion")
//    api("io.ktor:ktor-client-auth-jvm:$ktorVersion")

    api("io.minio:minio:$minioVersion")

    api(project(":core"))

//    testImplementation("org.testcontainers:testcontainers:$testContainersVer")
//    testImplementation("org.testcontainers:postgresql:$testContainersVer")
//    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
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

