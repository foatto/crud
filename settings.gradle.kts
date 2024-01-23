rootProject.name = "crud"

pluginManagement {
    val kotlinVersion: String by settings
    val agpVersion: String by settings
    val composePluginVersion: String by settings
    val springDependencyManagementPluginVersion: String by settings
    val springBootVersion: String by settings

    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("multiplatform").version(kotlinVersion)
        kotlin("js").version(kotlinVersion)
        kotlin("android").version(kotlinVersion)
        kotlin("jvm").version(kotlinVersion)

        kotlin("plugin.serialization").version(kotlinVersion)

        id("com.android.application").version(agpVersion)
        id("com.android.library").version(agpVersion)

        id("org.jetbrains.compose").version(composePluginVersion)

        kotlin("plugin.spring").version(kotlinVersion)
        kotlin("plugin.jpa").version(kotlinVersion)

        id("io.spring.dependency-management").version(springDependencyManagementPluginVersion)
        id("org.springframework.boot").version(springBootVersion)
    }
}

//--- возможно, это лишнее, т.к. есть блок subprojects.repositories в корневом build.gradle.kts
//dependencyResolutionManagement {
//    repositories {
//        google()
//        mavenCentral()
//        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
//    }
//}

include(
    ":core",
    ":core_compose",
    ":core_compose_web",
    ":core_server",

    ":mms_core",
    ":mms_compose_web",
    ":mms_compose",
    ":mms_server",

    ":office_core",
    ":office_compose_web",
    ":office_server",

    ":shop_core",
    ":shop_compose_web",
    ":shop_server",

    ":ts_core",
    ":ts_compose_web",
    ":ts_server",
)



