plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    implementation(project(":libs:connector"))
    implementation(project(":libs:telegram-kt"))

    implementation(libs.bundles.telegram) {
        exclude(module = "ktor-server")
        exclude(module = "ktor-server-host-common")
        exclude(module = "kotlinx-serialization-json")
        exclude(module = "kotlinx-serialization-core")
        exclude(module = "kotlinx-serialization-properties")
        exclude(module = "kotlinx-serialization-cbor")
    }

    implementation(libs.bundles.kotlinx.serialization)
    implementation(libs.spring.starter.webflux)

    developmentOnly(libs.spring.devtools)
    annotationProcessor(libs.spring.configuration.processor)

    testImplementation(libs.spring.starter.test)
}
