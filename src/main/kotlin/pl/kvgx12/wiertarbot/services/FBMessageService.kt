package pl.kvgx12.wiertarbot.services

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import pl.kvgx12.wiertarbot.Constants
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

    @Scheduled(cron="0 0 */6 * * ?")
    fun messageGarbageCollector() {
        val time = Instant.now().epochSecond - Constants.timeToRemoveSentMessages

        val attachments = fbMessageRepository.findAllByDeletedAtNullAndTimeBefore(time)
            .flatMap { json.decodeFromString<FBMessage>(it.message).attachments }

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
    data class FBMessageAttachment(
        val type: String,
        val filename: String? = null,
        val originalExtension: String? = null,
        val id: String? = null
    )

    @Serializable
    data class FBMessage(
        val attachments: List<FBMessageAttachment> = emptyList()
    )
}