package pl.kvgx12.wiertarbot.events

data class Mention(
    val threadId: String,
    val offset: Int,
    val length: Int
)
