import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektPlugin
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21

        configurations {
            compileOnly {
                extendsFrom(configurations.annotationProcessor.get())
            }
        }
    }

    tasks {
        // disable generating *-plain.jar
        withType<Jar> {
            if (projectDir.relativeTo(rootDir).startsWith("libs/")) {
                when (name) {
                    "jar" -> enabled = true
                    "bootJar" -> enabled = false
                }
            } else if (name == "jar" && findByName("bootJar") != null) {
                enabled = false
            }
        }

        withType<KotlinCompile> {
            compilerOptions {
                jvmTarget.assign(JvmTarget.JVM_21)
                freeCompilerArgs.add("-Xjsr305=strict")
            }
        }

        withType<JavaCompile> {
            targetCompatibility = "21"
            sourceCompatibility = "21"
        }

        withType<Test> {
            useJUnitPlatform()

            outputs.upToDateWhen { false }

            testLogging {
                showStandardStreams = true
                exceptionFormat = TestExceptionFormat.FULL
            }

            System.getProperties().asIterable()
                .filter { it.key != "user.dir" }
                .associateTo(systemProperties) { it.key.toString() to it.value }
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
