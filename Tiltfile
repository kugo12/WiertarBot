config.define_string_list("profiles")
cfg = config.parse()

PROFILES = {
    "telegram": ["connector-telegram", "build-connector-telegram"],
    "fb": ["connector-fb", "build-connector-fb"],
    "api": ["ttrs-api", "download-api", "build-download-api"],
    "core": ["core", "build-core"]
}
COMPOSE_LABELS = {
    "WiertarBot": ["core", "connector-telegram", "connector-fb"],
    "API": ["ttrs-api", "download-api"],
    "Infrastructure": ["minio", "rabbitmq", "db"],
}

resources = ["migration", "minio", "rabbitmq", "db"]
for profile in cfg.get("profiles"):
    resources += PROFILES[profile]

config.set_enabled_resources(resources)


def deps(path, base_deps=[]):
    return base_deps + ["%s/src" % path, "%s/build.gradle.kts" % path]

GRADLE_DEPS = ["gradle", "build.gradle.kts"]
PROTO_DEPS = deps("libs/core-proto", GRADLE_DEPS)
CONNECTOR_DEPS = deps("libs/connector", PROTO_DEPS)


def image(name):
    return "kugo12/%s" % name

def build_service(name, service_name, deps, label):
    local_resource(
        "build-%s" % service_name,
        "./gradlew :services:%s:bootJar" % service_name,
        deps=deps,
        allow_parallel=True,
        labels=[label],
    )

    docker_build(
        image(name),
        "services/%s" % service_name,
    )


build_service(
    name="wiertarbot",
    service_name="core",
    deps=deps("services/core", PROTO_DEPS),
    label="WiertarBot",
)

build_service(
    name="wiertarbot-connector-telegram",
    service_name="connector-telegram",
    deps=deps("services/connector-telegram", CONNECTOR_DEPS) + deps("libs/telegram-kt"),
    label="WiertarBot",
)

build_service(
    name="wiertarbot-connector-fb",
    service_name="connector-fb",
    deps= deps("services/connector-fb", CONNECTOR_DEPS) + deps("libs/fbchat-kt"),
    label="WiertarBot",
)

build_service(
    name="wiertarbot-download-api",
    service_name="download-api",
    deps=deps("services/download-api"),
    label="API",
)

docker_build(image("wiertarbot-ttrs-api"), "services/ttrs-api", platform="linux/amd64")
docker_build(image("wiertarbot-migration"), "services/migration")


docker_compose("dev/docker-compose.yml")

dc_resource("migration", labels=["WiertarBot"], trigger_mode=TRIGGER_MODE_MANUAL)
for label, services in COMPOSE_LABELS.items():
    for service in services:
        dc_resource(service, labels=[label])
