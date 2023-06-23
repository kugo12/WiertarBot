package pl.kvgx12.fbchat.data

import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
enum class ThreadLocation {
    INBOX, PENDING, ARCHIVED, OTHER;
}

@Serializable
data class ActiveStatus(
    val active: Boolean,
    val lastActive: Long?
)

@Serializable
data class Image(
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
)

@Serializable
enum class EmojiSize(val id: String) {
    Large("369239383222810"),
    Medium("369239343222814"),
    Small("369239263222822");
}

object FBTags {
    private val emojiSizeMap = mapOf(
        "large" to EmojiSize.Large,
        "small" to EmojiSize.Small,
        "medium" to EmojiSize.Medium,
        "m" to EmojiSize.Medium,
        "s" to EmojiSize.Small,
        "l" to EmojiSize.Large,
    )
    private val log = LoggerFactory.getLogger(FBTags::class.java)

    fun emojiSize(tags: List<String>): EmojiSize? {
        val tag = tags.find { it.startsWith("hot_emoji_size") }

        return tag?.let { t ->
            val sizeName = t.substringAfterLast(':')

            emojiSizeMap[sizeName].also {
                it ?: log.warn("Emoji size not found for name $sizeName, tags=$tags")
            }
        }
    }

    fun isForwarded(tags: List<String>): Boolean = tags.any {
        it.contains("forward") || it.contains("copy")
    }
}
