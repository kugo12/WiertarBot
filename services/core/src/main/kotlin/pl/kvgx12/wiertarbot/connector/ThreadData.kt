package pl.kvgx12.wiertarbot.connector

data class ThreadData(
    val id: String,
    val name: String,
    val messageCount: Long?,
    val participants: List<String>
)
