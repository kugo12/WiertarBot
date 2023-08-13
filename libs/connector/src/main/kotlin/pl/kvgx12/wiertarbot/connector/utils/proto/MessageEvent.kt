package pl.kvgx12.wiertarbot.connector.utils.proto

import pl.kvgx12.wiertarbot.proto.MessageEvent


val MessageEvent.isGroup get() = threadId != authorId
