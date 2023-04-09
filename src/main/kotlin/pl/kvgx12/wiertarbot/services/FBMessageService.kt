package pl.kvgx12.wiertarbot.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Scheduled
import pl.kvgx12.wiertarbot.Constants
import pl.kvgx12.wiertarbot.events.Mention
import pl.kvgx12.wiertarbot.repositories.FBMessageRepository
import java.time.Instant
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div

class FBMessageService(
    private val fbMessageRepository: FBMessageRepository
) {
    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
        }
    }

    fun decodeMessage(message: String): FBMessage = json.decodeFromString(message)

    suspend fun getDeletedMessages(threadId: String, count: Int): List<FBMessage> = withContext(Dispatchers.IO) {
        fbMessageRepository.findAllByThreadIdAndDeletedAtNotNull(
            threadId,
            PageRequest.of(
                0,
                count,
                Sort.by(Sort.Direction.DESC, "deletedAt")
            )
        ).map { decodeMessage(it.message) }
    }

    @Scheduled(cron = "0 0 */6 * * ?")
    fun messageGarbageCollector() {
        val time = Instant.now().epochSecond - Constants.timeToRemoveSentMessages

        val attachments = fbMessageRepository.findAllByDeletedAtNullAndTimeBefore(time)
            .flatMap { decodeMessage(it.message).attachments }

        for (attachment in attachments) {
            val path = Constants.attachmentSavePath / when {
                attachment.type == "ImageAttachment"
                        && attachment.id != null && attachment.originalExtension != null ->
                    "${attachment.id}.${attachment.originalExtension}"

                attachment.type == "AudioAttachment" && attachment.filename != null ->
                    attachment.filename

                attachment.type == "VideoAttachment" && attachment.id != null ->
                    "${attachment.id}.mp4"

                else -> continue
            }

            path.deleteIfExists()
        }

        fbMessageRepository.deleteByDeletedAtNullAndTimeBefore(time)
    }


    @Serializable
    data class FBMessage(
        val text: String? = null,
        val attachments: List<Attachment> = emptyList(),
        val mentions: List<Mention> = emptyList()
    ) {
        @Serializable
        data class Mention(
            @SerialName("thread_id")
            val threadId: String,
            val offset: Int,
            val length: Int
        )

        @Serializable
        data class Attachment(
            val type: String,
            val filename: String? = null,
            @SerialName("original_extension")
            val originalExtension: String? = null,
            val id: Long? = null
        )
    }
}
