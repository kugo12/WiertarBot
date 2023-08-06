plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.protobuf)
}

dependencies {
    api(libs.protobuf.kotlin)
    api(libs.protobuf.java)
}

protobuf.generateProtoTasks.all().configureEach {
    plugins.register("kotlin")
}
