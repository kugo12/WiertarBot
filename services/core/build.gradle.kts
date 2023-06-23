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
        }
    }
}

dependencies {
    implementation(project(":libs:fbchat-kt"))

    implementation(libs.bundles.spring)
    implementation(libs.spring.starter.data.jpa)
    implementation(libs.spring.starter.amqp)
    implementation(libs.spring.starter.cache)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.bundles.ktor.client)

    implementation(libs.caffeine)
    implementation(libs.jep)
    implementation(libs.inmo.telegram)
    implementation(libs.scrimage.core)
    implementation(libs.bundles.imageio)
    implementation(libs.skrape.html)

    runtimeOnly(libs.spring.postgresql)

    developmentOnly(libs.spring.devtools)
    annotationProcessor(libs.spring.configuration.processor)

    testImplementation(libs.spring.starter.test)
    testImplementation(libs.spring.rabbitmq.test)
    testImplementation(libs.kotlinx.benchmark.runtime)
}

tasks.bootJar {
    launchScript()
}
