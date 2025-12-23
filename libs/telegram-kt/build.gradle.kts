plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.bundles.kotlinx.serialization)

    implementation(libs.bundles.ktor.client)
    implementation(libs.ktor.client.resources)
    implementation(libs.ktor.client.websockets)

    implementation(libs.slf4j.api)

    testImplementation(libs.kotlin.test)
}
