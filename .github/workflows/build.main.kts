#!/usr/bin/env kotlin

@file:DependsOn("io.github.typesafegithub:github-workflows-kt:0.47.0")

import Build_main.JB
import io.github.typesafegithub.workflows.actions.actions.CheckoutV3
import io.github.typesafegithub.workflows.actions.actions.DownloadArtifactV3
import io.github.typesafegithub.workflows.actions.actions.SetupJavaV3
import io.github.typesafegithub.workflows.actions.actions.UploadArtifactV3
import io.github.typesafegithub.workflows.actions.docker.BuildPushActionV4
import io.github.typesafegithub.workflows.actions.docker.LoginActionV2
import io.github.typesafegithub.workflows.actions.docker.SetupBuildxActionV2
import io.github.typesafegithub.workflows.actions.gradle.GradleBuildActionV2
import io.github.typesafegithub.workflows.domain.Concurrency
import io.github.typesafegithub.workflows.domain.Container
import io.github.typesafegithub.workflows.domain.RunnerType.UbuntuLatest
import io.github.typesafegithub.workflows.domain.actions.CustomAction
import io.github.typesafegithub.workflows.domain.triggers.PullRequest
import io.github.typesafegithub.workflows.domain.triggers.Push
import io.github.typesafegithub.workflows.domain.triggers.WorkflowDispatch
import io.github.typesafegithub.workflows.dsl.JobBuilder
import io.github.typesafegithub.workflows.dsl.expressions.Contexts
import io.github.typesafegithub.workflows.dsl.expressions.expr
import io.github.typesafegithub.workflows.dsl.port
import io.github.typesafegithub.workflows.dsl.workflow
import io.github.typesafegithub.workflows.yaml.writeToFile

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

data class Service(val name: String, val image: String, val platforms: Set<String> = amd64)

val services = listOf(
    Service("core", "wiertarbot"),
    Service("download-api", "wiertarbot-download-api", amd64 + arm64),
    Service("ttrs-api", "wiertarbot-ttrs-api"),
)

workflow(
    name = "Build",
    on = listOf(
        Push(listOf(DEFAULT_BRANCH)),
        PullRequest(branches = listOf(DEFAULT_BRANCH)),
        WorkflowDispatch(),
    ),
    sourceFile = __FILE__.toPath(),
    concurrency = Concurrency(
        group = expr { github.ref },
        cancelInProgress = true,
    ),
) {
    val gradleBuild = job(
        id = "gradle-build",
        runsOn = UbuntuLatest,
    ) {
        checkout()
        setupJava()
        gradle("Gradle build", "assemble")
        uses(
            name = "Upload build artifacts",
            action = UploadArtifactV3(
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
                env = linkedMapOf(
                    "POSTGRES_USER" to "postgres",
                    "POSTGRES_PASSWORD" to "postgres",
                    "POSTGRES_DB" to "wiertarbot_test",
                ),
                ports = listOf(port(5432 to 5432)),
                options = Container.healthCheck("pg_isready"),
            ),
            "rabbitmq" to Container(
                image = "rabbitmq:3.12-alpine",
                env = linkedMapOf(
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
            action = UploadArtifactV3(
                name = "test-results",
                path = listOf(
                    "**/build/test-results",
                    "**/build/reports",
                ),
            ),
        )
    }

    val serviceJobs = services.map {
        val service = it.name
        val image = it.image
        val platforms = it.platforms

        job(
            id = "docker-$service",
            runsOn = UbuntuLatest,
            needs = listOf(gradleBuild),
        ) {
            checkout()

            if (platforms != amd64) uses(
                name = "Setup QEMU",
                action = CustomAction("docker", "setup-qemu-action", "v2"),
            )

            uses(name = "Setup docker buildx", action = SetupBuildxActionV2())
            uses(
                name = "Login to docker registry",
                condition = isMainAndPushOrDispatch,
                action = LoginActionV2(
                    username = expr(DOCKER_USERNAME),
                    password = expr(DOCKER_PASSWORD),
                ),
            )
            uses(
                name = "Download build artifacts",
                action = DownloadArtifactV3(name = "jars"),
            )
            uses(
                name = "Build docker image",
                action = BuildPushActionV4(
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
}.writeToFile()

typealias JB = JobBuilder<*>

fun JB.gradle(name: String, tasks: String) = uses(
    name = name,
    action = GradleBuildActionV2(
        arguments = tasks,
    ),
)

fun JB.checkout() = uses(
    name = "Check out",
    action = CheckoutV3(),
)

fun JB.setupJava() = uses(
    name = "Set up jdk",
    action = SetupJavaV3(
        javaVersion = "17",
        distribution = SetupJavaV3.Distribution.Temurin,
    ),
)
