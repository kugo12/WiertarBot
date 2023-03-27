package pl.kvgx12.wiertarbot.services

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import pl.kvgx12.wiertarbot.entities.Permission
import pl.kvgx12.wiertarbot.repositories.PermissionRepository
import pl.kvgx12.wiertarbot.utils.AllOpen
import java.util.*

@AllOpen
class PermissionDecoderService(
    private val permissionRepository: PermissionRepository,
) {
    fun findByCommand(command: String): Permission? = permissionRepository.findFirstByCommand(command)

    @Cacheable("permissions")
    fun getListsByCommand(command: String) =
        findByCommand(command)?.decodeLists()

    fun Permission.decodeLists(): Pair<PermissionList, PermissionList> =
        Json.decodeFromString<PermissionList>(whitelist) to Json.decodeFromString(blacklist)

    @CacheEvict("permissions", allEntries = true)
    fun clearCache() = Unit
}
