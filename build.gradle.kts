plugins {
    val kotlinVersion = "1.8.0"

    kotlin("jvm") version kotlinVersion apply false
    kotlin("plugin.spring") version kotlinVersion apply false
    kotlin("plugin.jpa") version kotlinVersion apply false
    kotlin("plugin.serialization") version kotlinVersion apply false
    kotlin("plugin.allopen") version kotlinVersion apply false

    id("org.springframework.boot") version "3.0.1" apply false
    id("io.spring.dependency-management") version "1.1.0" apply false
    id("org.jetbrains.kotlinx.benchmark") version "0.4.7" apply false
}

allprojects {
    group = "pl.kvgx12"
    version = ""

    repositories {
        mavenCentral()
    }
}
