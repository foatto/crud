rootProject.name = "crud"

pluginManagement {
    val kotlinPluginVersion: String by settings
    val springDependencyManagementPluginVersion: String by settings
    val springBootPluginVersion: String by settings

    plugins {
        kotlin("multiplatform").version(kotlinPluginVersion)
        kotlin("jvm").version(kotlinPluginVersion)
        kotlin("js").version(kotlinPluginVersion)

//        kotlin("plugin.allopen").version(kotlinVersion)
        kotlin("plugin.spring").version(kotlinPluginVersion)
//        kotlin("plugin.jpa").version(kotlinPluginVersion)

        id("io.spring.dependency-management").version(springDependencyManagementPluginVersion)
        id("org.springframework.boot").version(springBootPluginVersion)
    }
}

include(
    "core",
    "core_server",
    "core_web",

    "del_web",

//    "fs_server",

    "mms_server",
    "mms_web",

    "office_server",
    "office_web",

    "shop_server",
    "shop_web"
)



