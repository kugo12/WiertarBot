rootProject.name = "WiertarBot"

include(
    "services:core",
    "services:download-api",
    "services:connector-fb",
    "services:connector-telegram",

    "libs:fbchat-kt",
    "libs:core-proto",
    "libs:connector",
)
