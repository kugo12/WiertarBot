plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

allOpen {
    annotation("pl.kvgx12.wiertarbot.connector.utils.AllOpen")
}

dependencies {
    implementation(project(":libs:connector"))
    implementation(project(":libs:fbchat-kt"))

    implementation(libs.spring.starter.data.r2dbc)
    implementation(libs.spring.starter.cache)

    implementation(libs.bundles.kotlinx.serialization)
    implementation(libs.bundles.ktor.client)

    runtimeOnly(libs.spring.postgresql)
    runtimeOnly(libs.spring.postgresql.r2dbc)

    developmentOnly(libs.spring.devtools)
    annotationProcessor(libs.spring.configuration.processor)

    testImplementation(libs.spring.starter.test)
    testImplementation(libs.spring.rabbitmq.test)
    testImplementation(libs.bundles.kotest.spring)
    testImplementation(libs.mockk)
}
