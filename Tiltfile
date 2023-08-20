gradlew = "./gradlew "

gradle_stuff = ["gradle", "build.gradle.kts"]
proto_stuff = ["libs/core-proto"]
connector_stuff = gradle_stuff + proto_stuff + ["libs/connector"]

local_resource(
    "build-connector-telegram",
    gradlew + ":services:connector-telegram:bootJar",
    deps=connector_stuff + ["services/connector-telegram"]
)

local_resource(
    "build-connector-fb",
    gradlew + ":services:connector-fb:bootJar",
    deps=connector_stuff + ["services/connector-fb", "libs/fbchat-kt"]
)

local_resource(
    "build-core",
    gradlew + ":services:core:bootJar",
    deps=gradle_stuff + proto_stuff + ["services/core"]
)

local_resource(
    "build-download-api",
    gradlew + ":services:download-api:bootJar",
    deps=gradle_stuff + ["services/download-api"]
)

docker_build("kugo12/wiertarbot", "services/core")
docker_build("kugo12/wiertarbot-connector-telegram", "services/connector-telegram")
#docker_build("kugo12/wiertarbot-connector-fb", "services/connector-fb")
docker_build("kugo12/wiertarbot-ttrs-api", "services/ttrs-api", platform="linux/amd64")
docker_build("kugo12/wiertarbot-download-api", "services/download-api")

docker_compose("dev/docker-compose.yml")
