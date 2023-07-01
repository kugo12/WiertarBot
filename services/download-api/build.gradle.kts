plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.detekt)
}

dependencies {
    implementation(libs.bundles.spring.webflux)

    implementation(libs.bundles.ktor.client)
    implementation(libs.ktor.client.resources)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.java)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.bundles.s3)
    implementation(libs.jaffree)
    implementation(libs.skrape.html)
    implementation(libs.commons.codec)
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.spring.reactor.test)
    testImplementation(libs.spring.starter.test)

    developmentOnly(libs.spring.devtools)
}

tasks.bootJar {
    launchScript()
}
