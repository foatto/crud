val isBuildSupressWarning: String by project

val kotlinLanguageVersion: String by project
val kotlinApiVersion: String by project
val kotlinJvmTarget: String by project

plugins {
    kotlin("jvm")

//    kotlin("plugin.jpa")
    kotlin("plugin.spring")

    id("io.spring.dependency-management")
    id("org.springframework.boot")

    idea
}

dependencies {
    api(project(":core_server"))
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
            delete("/home/foatto/ShopServerSpring/lib")
            copy {
                from("build/libs/shop_server.jar", configurations["runtimeClasspath"])       // + snakeyaml-1.25.jar - ���� ����� ������ �������
                // from("build/libs/shop_server.jar", configurations["compileClasspath"])    // - snakeyaml-1.25.jar
                into("/home/foatto/ShopServerSpring/lib")
            }
        }
    }
}
