package pl.kvgx12.wiertarbot.fb.commands

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.springframework.beans.factory.BeanRegistrarDsl
import pl.kvgx12.wiertarbot.connector.DelegatedCommand
import pl.kvgx12.wiertarbot.fb.Constants
import pl.kvgx12.wiertarbot.fb.connector.FBContext
import pl.kvgx12.wiertarbot.fb.connector.FBMessageService
import pl.kvgx12.wiertarbot.proto.connector.sendTextRequest
import pl.kvgx12.wiertarbot.proto.connector.uploadRequest
import pl.kvgx12.wiertarbot.proto.mention
import pl.kvgx12.wiertarbot.proto.response
import kotlin.io.path.div

class DelegatedCommandsRegistrar : BeanRegistrarDsl({
    registerBean(DelegatedCommand.name("see")) {
        val fbMessageService = bean<FBMessageService>()
        val contextProvider = beanProvider<FBContext>()

        DelegatedCommand { event ->
            val context = contextProvider.first()
            val count = event.text.split(' ', limit = 2).last()
                .toIntOrNull()?.coerceIn(1, 10)
                ?: 1

            val messages = fbMessageService.getDeletedMessages(event.threadId, count)

            var isEmpty = true
            coroutineScope {
                messages.collect { message ->
                    isEmpty = false
                    val mentions = message.mentions.map {
                        mention {
                            threadId = it.threadId
                            offset = it.offset
                            length = it.length
                        }
                    }
                    var voiceClip = false
                    val attachments = message.attachments.mapNotNull {
                        when (it.type) {
                            "ImageAttachment" -> Constants.attachmentSavePath / "${it.id}.${it.originalExtension}"
                            "VideoAttachment" -> Constants.attachmentSavePath / "${it.id}.mp4"
                            "AudioAttachment" -> {
                                voiceClip = true
                                Constants.attachmentSavePath / "${it.filename}"
                            }

                            else -> null
                        }
                    }

                    launch {
                        val files = when {
                            attachments.isNotEmpty() -> context.upload(
                                uploadRequest {
                                    this.files += attachments.map { it.toString() }
                                    this.voiceClip = voiceClip
                                },
                            ).filesList

                            else -> emptyList()
                        }

                        context.send(
                            response {
                                this.event = event
                                message.text?.let { text = it }
                                this.mentions += mentions
                                this.files += files
                                this.voiceClip = voiceClip
                            },
                        )
                    }
                }
            }

            if (isEmpty) {
                context.sendText(
                    sendTextRequest {
                        this.event = event
                        text = "Nie ma żadnych zapisanych usuniętych wiadomości w tym wątku"
                    },
                )
            }
        }
    }
})
