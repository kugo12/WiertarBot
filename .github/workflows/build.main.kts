#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:Repository("https://bindings.krzeminski.it")

@file:DependsOn("io.github.typesafegithub:github-workflows-kt:3.6.0")

@file:DependsOn("actions:checkout:v6")
@file:DependsOn("actions:download-artifact:v6")
@file:DependsOn("actions:upload-artifact:v5")
@file:DependsOn("actions:setup-java:v5")
@file:DependsOn("docker:setup-buildx-action:v3")
@file:DependsOn("docker:setup-qemu-action:v3")
@file:DependsOn("docker:login-action:v3")
@file:DependsOn("docker:build-push-action:v6")
@file:DependsOn("dorny:paths-filter:v2")
@file:DependsOn("gradle:actions__setup-gradle:v5")


import io.github.typesafegithub.workflows.actions.actions.Checkout
import io.github.typesafegithub.workflows.actions.actions.DownloadArtifact
import io.github.typesafegithub.workflows.actions.actions.SetupJava
import io.github.typesafegithub.workflows.actions.actions.UploadArtifact
import io.github.typesafegithub.workflows.actions.docker.BuildPushAction
import io.github.typesafegithub.workflows.actions.docker.LoginAction
import io.github.typesafegithub.workflows.actions.docker.SetupBuildxAction
import io.github.typesafegithub.workflows.actions.docker.SetupQemuAction
import io.github.typesafegithub.workflows.actions.dorny.PathsFilter
import io.github.typesafegithub.workflows.actions.gradle.ActionsSetupGradle
import io.github.typesafegithub.workflows.domain.Concurrency
import io.github.typesafegithub.workflows.domain.Container
import io.github.typesafegithub.workflows.domain.RunnerType.UbuntuLatest
import io.github.typesafegithub.workflows.domain.triggers.PullRequest
import io.github.typesafegithub.workflows.domain.triggers.Push
import io.github.typesafegithub.workflows.domain.triggers.WorkflowDispatch
import io.github.typesafegithub.workflows.dsl.JobBuilder
import io.github.typesafegithub.workflows.dsl.expressions.Contexts
import io.github.typesafegithub.workflows.dsl.expressions.expr
import io.github.typesafegithub.workflows.dsl.port
import io.github.typesafegithub.workflows.dsl.workflow

val DEFAULT_BRANCH = "main"

val DOCKER_USERNAME by Contexts.secrets
val DOCKER_PASSWORD by Contexts.secrets
val DOCKERHUB_USER by Contexts.secrets
val GITLAB_WB_D_TOKEN by Contexts.secrets
val GITLAB_WB_D_URL by Contexts.secrets

val isMainAndPushOrDispatch = expr {
    "${github.ref_name} == '$DEFAULT_BRANCH' && (${github.event_name} == 'push' || ${github.event_name} == 'workflow_dispatch')"
}

val amd64 = setOf("linux/amd64")
val arm64 = setOf("linux/arm64")

data class Service(val name: String, val image: String, val paths: Set<String>, val platforms: Set<String> = amd64)

val gradleStuff = setOf("gradle/**", "settings.gradle.kts", "build.gradle.kts")
val libProto = setOf("libs/core-proto/**")
val libConnector = libProto + setOf("libs/connector/**")
val wbServices = listOf(
    Service(
        "core",
        "wiertarbot",
        gradleStuff + libProto + "services/core/**" + "libs/toon/**",
    ),
    Service(
        "connector-fb",
        "wiertarbot-connector-fb",
        gradleStuff + libConnector + setOf(
            "libs/fbchat-kt/**",
            "services/connector-fb/**",
        ),
    ),
    Service(
        "connector-telegram",
        "wiertarbot-connector-telegram",
        gradleStuff + libConnector + "services/connector-telegram/**",
    ),
    Service(
        "download-api",
        "wiertarbot-download-api",
        gradleStuff + "services/download-api/**",
        amd64 + arm64,
    ),
    Service(
        "ttrs-api",
        "wiertarbot-ttrs-api",
        setOf("services/ttrs-api/**"),
    ),
    Service(
        "cex-api",
        "wiertarbot-cex-api",
        setOf("services/cex-api/**"),
    ),
    Service(
        "migration",
        "wiertarbot-migration",
        setOf("services/migration/**"),
    ),
)

workflow(
    name = "Build",
    on = listOf(
        Push(listOf(DEFAULT_BRANCH)),
        PullRequest(branches = listOf(DEFAULT_BRANCH)),
        WorkflowDispatch(
            inputs = mapOf(
                "services" to WorkflowDispatch.Input(
                    "Services to build",
                    true,
                    WorkflowDispatch.Type.String,
                    default = wbServices.joinToString(",") { it.name },
                ),
            ),
        ),
    ),
    sourceFile = __FILE__,
    concurrency = Concurrency(
        group = expr { github.ref },
        cancelInProgress = true,
    ),
) {
    val changes = job(
        id = "detect-changes",
        runsOn = UbuntuLatest,
        _customArguments = mapOf(
            "outputs" to wbServices
                .filter { it.paths.isNotEmpty() }
                .associate { it.name to expr("steps.step-1.outputs.${it.name}") },
        ),
    ) {
        checkout()
        uses(
            name = "Filter",
            action = PathsFilter(
                filters = buildString {
                    wbServices
                        .filter { it.paths.isNotEmpty() }
                        .forEach {
                            append(it.name, ":\n")
                            it.paths.forEach { path ->
                                append("  - '", path, "'\n")
                            }
                        }
                },
            ),
        )
    }

    val gradleBuild = job(
        id = "gradle-build",
        runsOn = UbuntuLatest,
    ) {
        checkout()
        setupJava()
        gradle("Gradle build", "assemble")
        uses(
            name = "Upload build artifacts",
            action = UploadArtifact(
                name = "jars",
                path = listOf("**/build/libs"),
            ),
        )
    }

    job(
        id = "gradle-test",
        runsOn = UbuntuLatest,
        needs = listOf(gradleBuild),
        services = mapOf(
            "postgres" to Container(
                image = "postgres:14-alpine",
                env = mapOf(
                    "POSTGRES_USER" to "postgres",
                    "POSTGRES_PASSWORD" to "postgres",
                    "POSTGRES_DB" to "wiertarbot_test",
                ),
                ports = listOf(port(5432 to 5432)),
                options = Container.healthCheck("pg_isready"),
            ),
            "rabbitmq" to Container(
                image = "rabbitmq:3.12-alpine",
                env = mapOf(
                    "RABBITMQ_DEFAULT_USER" to "guest",
                    "RABBITMQ_DEFAULT_PASS" to "guest",
                ),
                ports = listOf(port(5672 to 5672)),
                options = Container.healthCheck("rabbitmq-diagnostics -q ping"),
            ),
        ),
    ) {
        checkout()
        setupJava()
        gradle("Gradle test", "test --continue")
        uses(
            name = "Upload test artifacts",
            condition = expr { always() },
            action = UploadArtifact(
                name = "test-results",
                path = listOf(
                    "**/build/test-results",
                    "**/build/reports",
                ),
            ),
        )
    }

    val serviceJobs = wbServices.map {
        val service = it.name
        val image = it.image
        val platforms = it.platforms

        job(
            id = "docker-$service",
            runsOn = UbuntuLatest,
            needs = listOf(gradleBuild, changes),
            condition = when {
                it.paths.isNotEmpty() -> expr {
                    "needs.${changes.id}.outputs.${it.name} == 'true' || contains(github.event.inputs.services, '${it.name}')"
                }

                else -> null
            },
        ) {
            checkout()

            if (platforms != amd64) uses(
                name = "Setup QEMU",
                action = SetupQemuAction(),
            )

            uses(name = "Setup docker buildx", action = SetupBuildxAction())
            uses(
                name = "Login to docker registry",
                condition = isMainAndPushOrDispatch,
                action = LoginAction(
                    username = expr(DOCKER_USERNAME),
                    password = expr(DOCKER_PASSWORD),
                ),
            )
            uses(
                name = "Download build artifacts",
                action = DownloadArtifact(name = "jars"),
            )
            uses(
                name = "Build docker image",
                action = BuildPushAction(
                    context = "services/$service",
                    file = "services/$service/Dockerfile",
                    platforms = platforms.toList(),
                    tags = listOf(
                        "${expr(DOCKERHUB_USER)}/$image:latest",
                        "${expr(DOCKERHUB_USER)}/$image:ci-${expr { github.run_id }}",
                    ),
                    _customInputs = mapOf("push" to isMainAndPushOrDispatch),
                ),
            )
        }
    }

    job(
        id = "trigger-deployment",
        runsOn = UbuntuLatest,
        needs = serviceJobs,
        condition = isMainAndPushOrDispatch,
        _customArguments = mapOf("environment" to "production"),
    ) {
        run(
            name = "Trigger deployment",
            command = """
                curl -fX POST \
                    -F "token=${expr(GITLAB_WB_D_TOKEN)}" \
                    -F ref=main \
                    -F "variables[CI_SOURCE]=WiertarBot" \
                    "${expr(GITLAB_WB_D_URL)}" > /dev/null
            """.trimIndent(),
        )
    }
}

typealias JB = JobBuilder<*>

fun JB.gradle(name: String, tasks: String) {
    uses(
        name = "Setup gradle",
        action = ActionsSetupGradle(),
    )
    run(name = name, command = "./gradlew $tasks")
}

fun JB.checkout() = uses(
    name = "Check out",
    action = Checkout(),
)

fun JB.setupJava() = uses(
    name = "Set up jdk",
    action = SetupJava(
        javaVersion = "21",
        distribution = SetupJava.Distribution.Temurin,
    ),
)
