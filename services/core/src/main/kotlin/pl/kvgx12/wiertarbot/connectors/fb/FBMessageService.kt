package pl.kvgx12.wiertarbot.connectors.fb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Scheduled
import pl.kvgx12.wiertarbot.Constants
import pl.kvgx12.wiertarbot.repositories.FBMessageRepository
import java.time.Instant
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div

class FBMessageService(
    private val fbMessageRepository: FBMessageRepository,
) {
    fun getDeletedMessages(threadId: String, count: Int): Flow<FBMessageSerialization.FBMessage> =
        fbMessageRepository.findAllByThreadIdAndDeletedAtNotNull(
            threadId,
            PageRequest.of(
                0,
                count,
                Sort.by(Sort.Direction.DESC, "deletedAt"),
            ),
        ).map {
            FBMessageSerialization.deserializeMessageEvent(it.message)
        }

    @OptIn(FlowPreview::class)
    @Scheduled(cron = "0 0 */6 * * ?")
    fun messageGarbageCollector() = runBlocking {
        val time = Instant.now().epochSecond - Constants.timeToRemoveSentMessages

        fbMessageRepository.findAllByDeletedAtNullAndTimeBefore(time)
            .flatMapConcat { FBMessageSerialization.deserializeMessageEvent(it.message).attachments.asFlow() }
            .collect { attachment ->
                val path = Constants.attachmentSavePath / when {
                    attachment.type == "ImageAttachment" &&
                        attachment.id != null &&
                        attachment.originalExtension != null ->
                        "${attachment.id}.${attachment.originalExtension}"

                    attachment.type == "AudioAttachment" && attachment.filename != null ->
                        attachment.filename

                    attachment.type == "VideoAttachment" && attachment.id != null ->
                        "${attachment.id}.mp4"

                    else -> return@collect
                }

                launch(Dispatchers.IO) {
                    path.deleteIfExists()
                }
            }

        fbMessageRepository.deleteByDeletedAtNullAndTimeBefore(time)
    }
}
