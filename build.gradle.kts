plugins {
    kotlin("multiplatform").apply(false)
    kotlin("js").apply(false)
    kotlin("android").apply(false)
    kotlin("jvm").apply(false)

    kotlin("plugin.serialization").apply(false)

    id("com.android.application").apply(false)
    id("com.android.library").apply(false)

    id("org.jetbrains.compose").apply(false)

    kotlin("plugin.spring").apply(false)
    kotlin("plugin.jpa").apply(false)

    id("io.spring.dependency-management").apply(false)
    id("org.springframework.boot").apply(false)
}

//--- allprojects не нужен, в корневом "проекте" нет исходников
subprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
