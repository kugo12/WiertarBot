plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.bundles.kotlinx.serialization)

    api(libs.bundles.ktor.client)
    api(libs.ktor.client.resources)

    implementation(libs.slf4j.api)

    testImplementation(libs.kotlin.test)
}
