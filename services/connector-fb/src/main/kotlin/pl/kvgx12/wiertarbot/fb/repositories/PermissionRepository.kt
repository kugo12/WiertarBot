package pl.kvgx12.wiertarbot.fb.repositories

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import pl.kvgx12.wiertarbot.fb.entities.Permission

@Repository
interface PermissionRepository : CoroutineCrudRepository<Permission, Int> {
    suspend fun findFirstByCommand(command: String): Permission?
}
