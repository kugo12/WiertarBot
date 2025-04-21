plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    implementation(project(":libs:connector"))

    implementation(libs.bundles.telegram) {
        exclude(module = "ktor-server")
        exclude(module = "ktor-server-host-common")
        exclude(module = "kotlinx-serialization-json")
        exclude(module = "kotlinx-serialization-core")
        exclude(module = "kotlinx-serialization-properties")
        exclude(module = "kotlinx-serialization-cbor")
    }

    // hack to force version
    val serializationVersion = libs.versions.kotlinx.serialization.get()
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$serializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:$serializationVersion")

    developmentOnly(libs.spring.devtools)
    annotationProcessor(libs.spring.configuration.processor)

    testImplementation(libs.spring.starter.test)
}

tasks.bootJar {
    launchScript()
}
