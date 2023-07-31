package pl.kvgx12.wiertarbot.repositories

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import pl.kvgx12.wiertarbot.entities.Permission

@Repository
interface PermissionRepository : CoroutineCrudRepository<Permission, Int> {
    suspend fun findFirstByCommand(command: String): Permission?
}
