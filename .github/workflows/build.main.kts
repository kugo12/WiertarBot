#!/usr/bin/env kotlin

@file:DependsOn("io.github.typesafegithub:github-workflows-kt:0.45.0")

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
import io.github.typesafegithub.workflows.domain.RunnerType.UbuntuLatest
import io.github.typesafegithub.workflows.domain.triggers.PullRequest
import io.github.typesafegithub.workflows.domain.triggers.Push
import io.github.typesafegithub.workflows.domain.triggers.WorkflowDispatch
import io.github.typesafegithub.workflows.dsl.JobBuilder
import io.github.typesafegithub.workflows.dsl.expressions.Contexts
import io.github.typesafegithub.workflows.dsl.expressions.expr
import io.github.typesafegithub.workflows.dsl.workflow
import io.github.typesafegithub.workflows.yaml.writeToFile

val DEFAULT_BRANCH = "main"

val DOCKER_USERNAME by Contexts.secrets
val DOCKER_PASSWORD by Contexts.secrets
val DOCKERHUB_USER by Contexts.secrets
val GITLAB_WB_D_TOKEN by Contexts.secrets
val GITLAB_WB_D_URL by Contexts.secrets

val isMainAndPush = expr {
    "${github.ref_name} == '${DEFAULT_BRANCH}' && ${github.event_name} == 'push'"
}

val services = mapOf(
    "core" to "wiertarbot",
    "download-api" to "wiertarbot-download-api",
    "ttrs-api" to "wiertarbot-ttrs-api",
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
    )
) {
    val gradleBuild = job(
        id = "gradle-build", runsOn = UbuntuLatest
    ) {
        checkout()
        setupJava()
        gradle("Gradle build", "assemble")
        uses(
            name = "Upload build artifacts",
            action = UploadArtifactV3(
                name = "jars",
                path = listOf("**/build/libs")
            )
        )
    }

    job(id = "gradle-test", runsOn = UbuntuLatest, needs = listOf(gradleBuild)) {
        checkout()
        setupJava()
        gradle("Gradle test", "test")
        uses(
            name = "Upload test artifacts",
            action = UploadArtifactV3(
                name = "test-results",
                path = listOf(
                    "**/build/test-results",
                    "**/build/reports",
                )
            )
        )
    }

    val serviceJobs = services.map { (service, image) ->
        job(
            id = "docker-$service",
            runsOn = UbuntuLatest,
            needs = listOf(gradleBuild),
        ) {
            checkout()
            setupDocker()
            uses(
                name = "Download build artifacts",
                action = DownloadArtifactV3(name = "jars")
            )
            uses(
                name = "Build and push docker image",
                action = BuildPushActionV4(
                    context = "services/$service",
                    file = "services/$service/Dockerfile",
                    platforms = listOf("linux/amd64"),
                    tags = listOf(
                        "${expr(DOCKERHUB_USER)}/${image}:latest",
                        "${expr(DOCKERHUB_USER)}/${image}:ci-${expr { github.run_id }}",
                    ),
                    _customInputs = mapOf("push" to isMainAndPush)
                )
            )
        }
    }

    job(
        id = "trigger-deployment",
        runsOn = UbuntuLatest,
        needs = serviceJobs,
        condition = isMainAndPush,
    ) {
        run(
            name = "Trigger deployment",
            command = """
                curl -fX POST \
                  -F "token=${expr(GITLAB_WB_D_TOKEN)}" \
                  -F ref=main \
                  -F "variables[CI_SOURCE]=WiertarBot" \
                  "${expr(GITLAB_WB_D_URL)}" > /dev/null
            """.trimIndent()
        )
    }
}.writeToFile()


typealias JB = JobBuilder<*>

fun JB.gradle(name: String, tasks: String) = uses(
    name = name,
    action = GradleBuildActionV2(
        arguments = tasks
    )
)


fun JB.checkout() = uses(
    name = "Check out",
    action = CheckoutV3()
)

fun JB.setupJava() = uses(
    name = "Set up jdk",
    action = SetupJavaV3(
        javaVersion = "17",
        distribution = SetupJavaV3.Distribution.Temurin,
    )
)

fun JB.setupDocker() {
    uses(name = "Setup docker buildx", action = SetupBuildxActionV2())
    uses(
        name = "Login to docker registry",
        condition = isMainAndPush,
        action = LoginActionV2(
            username = expr(DOCKER_USERNAME),
            password = expr(DOCKER_PASSWORD),
        )
    )
}
