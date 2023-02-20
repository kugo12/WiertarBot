package pl.kvgx12.wiertarbot.services

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.stereotype.Service
import org.springframework.util.ConcurrentLruCache
import pl.kvgx12.wiertarbot.entities.Permission
import pl.kvgx12.wiertarbot.repositories.PermissionRepository
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.jvm.optionals.getOrNull

@Service
class PermissionDecoderService(
    private val permissionRepository: PermissionRepository,
) {
    fun findByCommand(command: String): Permission? = permissionRepository.findFirstByCommand(command)

    @Cacheable("permissions")
    fun getListsByCommand(command: String) =
        findByCommand(command)?.decodeLists()

    fun Permission.decodeLists(): Pair<PermissionList, PermissionList> =
        Json.decodeFromString<PermissionList>(whitelist) to Json.decodeFromString(blacklist)
}
