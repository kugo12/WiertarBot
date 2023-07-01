plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.kotlinx.benchmark)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.detekt)
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

benchmark {
    targets {
        register("test")
    }
    configurations {
        "main" {
            warmups = 5
            iterations = 5
            iterationTime = 5
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.bundles.ktor.client)
    implementation(libs.ktor.client.resources)
    implementation(libs.ktor.client.websockets)

    implementation(libs.skrape.html)

    testImplementation(libs.logback)
    testImplementation(libs.logback.classic)
    testImplementation(libs.kotlinx.benchmark.runtime)
    testImplementation(libs.kotlin.test)
}
