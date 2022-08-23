rootProject.name = "crud"

pluginManagement {
    val kotlinVersion: String by settings
//    val composePluginVersion: String by settings
    val springDependencyManagementPluginVersion: String by settings
    val springBootVersion: String by settings

//    repositories {
//        google()
//        gradlePluginPortal()
//        mavenCentral()
//        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
//    }

    plugins {
        kotlin("multiplatform").version(kotlinVersion)
        kotlin("js").version(kotlinVersion)
//        kotlin("android").version(kotlinVersion)
        kotlin("jvm").version(kotlinVersion)

//        id("org.jetbrains.compose").version(composePluginVersion)

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
    ":core_server",
    ":core_web",

//    ":core_compose",
//    ":core_compose_web",

    ":mms_core",
    ":mms_server",
    ":mms_web",
//    ":mms_compose_web",

    ":office_core",
    ":office_server",
    ":office_web",

    ":shop_core",
    ":shop_server",
    ":shop_web",

    ":ts_core",
    ":ts_server",
    ":ts_web"
)



