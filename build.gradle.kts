import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jmailen.gradle.kotlinter.KotlinterPlugin

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.detekt)
}

allprojects {
    apply<DetektPlugin>()
    apply<KotlinterPlugin>()

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
            if (name == "jar" && findByName("bootJar") != null) {
                enabled = false
            }
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

        withType<Detekt> {
            reports.sarif.required.set(true)
        }
    }

    kotlinter {
        reporters = arrayOf("sarif")
    }

    repositories {
        mavenCentral()
    }
}
