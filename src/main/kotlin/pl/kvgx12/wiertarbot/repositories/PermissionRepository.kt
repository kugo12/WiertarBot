package pl.kvgx12.wiertarbot.repositories

import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import pl.kvgx12.wiertarbot.entities.Permission

@Repository
interface PermissionRepository: R2dbcRepository<Permission, Int> {
    suspend fun findFirstByCommand(command: String): Permission?
}