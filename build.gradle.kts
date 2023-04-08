import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.8.0"
    id("org.springframework.boot") version "3.0.1"
    id("io.spring.dependency-management") version "1.1.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    kotlin("plugin.allopen") version kotlinVersion
    id("org.jetbrains.kotlinx.benchmark") version "0.4.7"
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
    annotation("pl.kvgx12.wiertarbot.utils.AllOpen")
}

group = "pl.kvgx12"
version = "0.0.1"
java.sourceCompatibility = JavaVersion.VERSION_17

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

benchmark {
    targets {
        register("test")
    }
    configurations {
        "main" {
            warmups = 3
            iterations = 3
            iterationTime = 5
        }
    }
}


dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    implementation("com.github.ben-manes.caffeine:caffeine:3.1.2")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    implementation("black.ninia:jep:4.1.1")
    implementation("io.sentry:sentry-spring-boot-starter-jakarta:6.14.0")
    implementation("io.sentry:sentry-logback:6.14.0")

    implementation("dev.inmo:tgbotapi.core:5.2.1")

    implementation("com.sksamuel.scrimage:scrimage-core:4.0.33")
    implementation("com.twelvemonkeys.imageio:imageio-core:3.9.4")
    implementation("com.twelvemonkeys.imageio:imageio:3.9.4")
    implementation("com.twelvemonkeys.imageio:imageio-jpeg:3.9.3")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("io.ktor:ktor-client-core:2.2.4")
    implementation("io.ktor:ktor-client-cio:2.2.4")
    implementation("io.ktor:ktor-client-content-negotiation:2.2.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.4")

    implementation("it.skrape:skrapeit-html-parser:1.2.2")

//    implementation("com.google.cloud:google-cloud-texttospeech:2.7.0")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    runtimeOnly("org.postgresql:postgresql")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.amqp:spring-rabbit-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.7")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// disable generating *-plain.jar
tasks.jar {
    enabled = false
}

tasks.bootJar {
    launchScript()
}
