plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    api(project(":libs:core-proto"))

    api(libs.bundles.spring)
    api(libs.spring.starter.amqp)
    api(libs.spring.kotlinx.coroutines.reactor)
    api(libs.kotlinx.coroutines.core)
}
