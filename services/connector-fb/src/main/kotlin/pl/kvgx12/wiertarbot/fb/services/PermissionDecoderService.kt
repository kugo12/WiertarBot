package pl.kvgx12.wiertarbot.fb.services

import kotlinx.serialization.json.Json
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import pl.kvgx12.wiertarbot.fb.entities.Permission
import pl.kvgx12.wiertarbot.fb.repositories.PermissionRepository
import pl.kvgx12.wiertarbot.connector.utils.AllOpen

@AllOpen
class PermissionDecoderService(
    private val permissionRepository: PermissionRepository,
) {
    suspend fun findByCommand(command: String): Permission? = permissionRepository.findFirstByCommand(command)

    @Cacheable("permissions")
    suspend fun getListsByCommand(command: String) =
        findByCommand(command)?.decodeLists()

    fun Permission.decodeLists(): Pair<PermissionList, PermissionList> =
        Json.decodeFromString<PermissionList>(whitelist) to Json.decodeFromString(blacklist)

    @CacheEvict("permissions", allEntries = true)
    fun clearCache() = Unit
}
