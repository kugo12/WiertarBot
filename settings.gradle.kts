rootProject.name = "WiertarBot"

include(
    "services:core",
    "services:download-api",
    "services:connector-fb",
    "services:connector-telegram",

    "libs:fbchat-kt",
    "libs:telegram-kt",
    "libs:core-proto",
    "libs:connector",
    "libs:toon",
)
