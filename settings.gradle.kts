rootProject.name = "crud"

pluginManagement {
    val kotlinVersion: String by settings
    val composePluginVersion: String by settings
    val springDependencyManagementPluginVersion: String by settings
    val springBootVersion: String by settings

//    repositories {
//        gradlePluginPortal()
//        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
//--- по документации вроде как не нужны
//        mavenCentral()
//        google()
//    }

    plugins {
        kotlin("multiplatform").version(kotlinVersion)
        kotlin("js").version(kotlinVersion)
//        kotlin("android").version(kotlinVersion)
        kotlin("jvm").version(kotlinVersion)

        kotlin("plugin.serialization").version(kotlinVersion)

        id("org.jetbrains.compose").version(composePluginVersion)

//--- in mpp part
//        id("com.android.library").version(extra["agp.version"] as String)

//        id("com.android.application").version(extra["agp.version"] as String)

        kotlin("plugin.spring").version(kotlinVersion)
        kotlin("plugin.jpa").version(kotlinVersion)

        id("io.spring.dependency-management").version(springDependencyManagementPluginVersion)
        id("org.springframework.boot").version(springBootVersion)
    }
}

include(
    ":core",
    ":core_web",
    ":core_compose",
    ":core_compose_web",
    ":core_server",
    ":core_server_mvc",
//    ":core_server_flux",

    ":mms_core",
    ":mms_web",
    ":mms_compose_web",
    ":mms_server",
    ":mms_server_mvc",
//    ":mms_server_flux",

    ":office_core",
    ":office_web",
    ":office_compose_web",
    ":office_server",
    ":office_server_mvc",
//    ":office_server_flux",

    ":shop_core",
    ":shop_web",
    ":shop_compose_web",
    ":shop_server",
    ":shop_server_mvc",
//    ":shop_server_flux",

    ":ts_core",
    ":ts_web",
    ":ts_compose_web",
    ":ts_server",
    ":ts_server_mvc",
//    ":ts_server_flux",
)



