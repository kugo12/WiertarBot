package pl.kvgx12.wiertarbot.connectors.fb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Scheduled
import pl.kvgx12.wiertarbot.Constants
import pl.kvgx12.wiertarbot.repositories.FBMessageRepository
import java.time.Instant
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div

class FBMessageService(
    private val fbMessageRepository: FBMessageRepository
) {
    suspend fun getDeletedMessages(threadId: String, count: Int): List<FBMessageSerialization.FBMessage> =
        withContext(Dispatchers.IO) {
            fbMessageRepository.findAllByThreadIdAndDeletedAtNotNull(
                threadId,
                PageRequest.of(
                    0,
                    count,
                    Sort.by(Sort.Direction.DESC, "deletedAt")
                )
            ).map { FBMessageSerialization.deserializeMessageEvent(it.message) }
        }

    @Scheduled(cron = "0 0 */6 * * ?")
    fun messageGarbageCollector() {
        val time = Instant.now().epochSecond - Constants.timeToRemoveSentMessages

        val attachments = fbMessageRepository.findAllByDeletedAtNullAndTimeBefore(time)
            .flatMap { FBMessageSerialization.deserializeMessageEvent(it.message).attachments }

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


}
