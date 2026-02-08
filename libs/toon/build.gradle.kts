plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.bundles.kotlinx.serialization)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotest.junit5)
    testImplementation(libs.kotest.assertions.core)
}
