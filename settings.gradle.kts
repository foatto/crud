rootProject.name = "crud"

pluginManagement {
    val kotlinVersion: String by settings
    val springDependencyManagementPluginVersion: String by settings
    val springBootVersion: String by settings

    plugins {
        kotlin("multiplatform").version(kotlinVersion)
        kotlin("jvm").version(kotlinVersion)
        kotlin("js").version(kotlinVersion)

        kotlin("plugin.spring").version(kotlinVersion)
        kotlin("plugin.jpa").version(kotlinVersion)

        id("io.spring.dependency-management").version(springDependencyManagementPluginVersion)
        id("org.springframework.boot").version(springBootVersion)
    }
}

include(
    "core",
    "core_server",
    "core_web",

    "mms_core",
    "mms_server",
    "mms_web",

    "office_core",
    "office_server",
    "office_web",

    "shop_core",
    "shop_server",
    "shop_web"
)



