import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
}

allprojects {
    group = "pl.kvgx12"
    version = ""

    extensions.findByType<JavaPluginExtension>()?.apply {
        sourceCompatibility = JavaVersion.VERSION_17

        configurations {
            compileOnly {
                extendsFrom(configurations.annotationProcessor.get())
            }
        }
    }

    tasks {
        // disable generating *-plain.jar
        withType<Jar> {
            if (name == "jar")
                enabled = false
        }

        withType<KotlinCompile> {
            kotlinOptions {
                freeCompilerArgs = listOf("-Xjsr305=strict")
                jvmTarget = "17"
            }
        }

        withType<Test> {
            useJUnitPlatform()
        }
    }

    repositories {
        mavenCentral()
    }
}
