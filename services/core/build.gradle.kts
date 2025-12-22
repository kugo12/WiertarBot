plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlinx.benchmark)
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
    annotation("pl.kvgx12.wiertarbot.utils.AllOpen")
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
            iterationTimeUnit = "m"
        }
    }
}

tasks {
    withType<Test> {
        systemProperty("kotest.framework.config.fqn", "pl.kvgx12.wiertarbot.ProjectConfig")
    }
}

dependencies {
    implementation(platform(libs.spring.ai.bom))

    implementation(project(":libs:core-proto"))

    implementation(libs.bundles.spring)
    implementation(libs.spring.starter.web)
    implementation(libs.spring.starter.webflux)
    implementation(libs.spring.starter.data.r2dbc)
    implementation(libs.spring.starter.amqp)
    implementation(libs.spring.starter.cache)
    implementation(libs.spring.ai.starter.google.genai)

    implementation(libs.spring.kotlinx.coroutines.core)
    implementation(libs.spring.kotlinx.coroutines.reactor)

    implementation(libs.kotlinx.datetime)
    implementation(libs.bundles.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.cbor)

    implementation(libs.advrieze.serialization.xml) {
        exclude("org.jetbrains.kotlinx", "kotlinx-serialization-json")
        exclude("org.jetbrains.kotlinx", "kotlinx-serialization-core")
    }

    implementation(libs.caffeine)
    implementation(libs.scrimage.core)
    implementation(libs.bundles.imageio)
    implementation(libs.skrape.html)

    runtimeOnly(libs.spring.postgresql)
    runtimeOnly(libs.spring.postgresql.r2dbc)
    if ("Mac" in System.getProperty("os.name")) {
        runtimeOnly(libs.netty.all)
    }

    developmentOnly(libs.spring.devtools)
    annotationProcessor(libs.spring.configuration.processor)

    testImplementation(libs.spring.starter.test)
    testImplementation(libs.spring.rabbitmq.test)
    testImplementation(libs.kotlinx.benchmark.runtime)
    testImplementation(libs.bundles.kotest.spring)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.spring)
}
