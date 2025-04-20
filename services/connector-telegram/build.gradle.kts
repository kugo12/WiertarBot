plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    implementation(project(":libs:connector"))

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.bundles.ktor.client)

    implementation(libs.caffeine)
    implementation(libs.bundles.telegram)
    implementation(libs.scrimage.core)
    implementation(libs.bundles.imageio)
    implementation(libs.skrape.html)

    runtimeOnly(libs.spring.postgresql)
    runtimeOnly(libs.spring.postgresql.r2dbc)

    developmentOnly(libs.spring.devtools)
    annotationProcessor(libs.spring.configuration.processor)

    testImplementation(libs.spring.starter.test)
    testImplementation(libs.spring.rabbitmq.test)
    testImplementation(libs.kotlinx.benchmark.runtime)
    testImplementation(libs.bundles.kotest.spring)
    testImplementation(libs.mockk)
}

tasks.bootJar {
    launchScript()
}
