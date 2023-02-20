package pl.kvgx12.wiertarbot.repositories

import org.springframework.cache.annotation.Cacheable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pl.kvgx12.wiertarbot.entities.Permission

@Repository
interface PermissionRepository: JpaRepository<Permission, Int> {
    fun findFirstByCommand(command: String): Permission?
}