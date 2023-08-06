import com.google.protobuf.gradle.id

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.protobuf)
}

dependencies {
    api(libs.protobuf.kotlin)
    api(libs.protobuf.java)
    api(libs.grpc.kotlin.stub)
    api(libs.grpc.protobuf)
    api(libs.grpc.netty.shaded)
}

protobuf {
    generateProtoTasks.all().configureEach {
        plugins {
            id("grpc")
            id("grpckt")
        }
        builtins {
            id("kotlin")
        }
    }

    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }

    plugins {
        id("grpc") {
            artifact = libs.grpc.protoc.java.get().toString()
        }
        id("grpckt") {
            artifact = libs.grpc.protoc.kotlin.get().toString() + ":jdk8@jar"
        }
    }
}
