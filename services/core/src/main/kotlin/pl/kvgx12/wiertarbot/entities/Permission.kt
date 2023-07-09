package pl.kvgx12.wiertarbot.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table
data class Permission(
    @Id
    val id: Int? = null,
    val command: String,
    var whitelist: String,
    var blacklist: String,
)
