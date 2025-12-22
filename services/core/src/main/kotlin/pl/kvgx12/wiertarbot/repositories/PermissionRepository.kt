package pl.kvgx12.wiertarbot.repositories

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import pl.kvgx12.wiertarbot.entities.Permission

interface PermissionRepository : CoroutineCrudRepository<Permission, Int> {
    suspend fun findFirstByCommand(command: String): Permission?
}
