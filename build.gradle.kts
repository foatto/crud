plugins {
    kotlin("multiplatform").apply(false)
    kotlin("jvm").apply(false)
    kotlin("js").apply(false)

//    kotlin("plugin.allopen").apply(false)
    kotlin("plugin.spring").apply(false)
//    kotlin("plugin.jpa").apply(false)

    id("io.spring.dependency-management").apply(false)
    id("org.springframework.boot").apply(false)
}

//--- allprojects не нужен, в корневом "проекте" нет исходников
subprojects {
    repositories {
        mavenCentral()
//        jcenter()
    }
}


