package pl.kvgx12.wiertarbot.services

import pl.kvgx12.wiertarbot.config.CacheService
import pl.kvgx12.wiertarbot.config.ContextHolder
import pl.kvgx12.wiertarbot.proto.ConnectorType
import pl.kvgx12.wiertarbot.proto.ThreadData
import pl.kvgx12.wiertarbot.proto.ThreadParticipant
import pl.kvgx12.wiertarbot.utils.ThreadDataSerializer

class CachedContextService(
    cacheService: CacheService,
    private val contextHolder: ContextHolder,
) {
    private val threadCache = cacheService["threads"]

    suspend fun getThread(connector: ConnectorType, threadId: String): ThreadData? =
        threadCache.putIfAbsent(connector.name + threadId, ThreadDataSerializer) {
            contextHolder[connector].fetchThread(threadId) ?: return null
        }

    suspend fun getThreadParticipant(connector: ConnectorType, threadId: String, userId: String): ThreadParticipant? =
        getThread(connector, threadId)
            ?.participantsList
            ?.find { it.id == userId }
}
