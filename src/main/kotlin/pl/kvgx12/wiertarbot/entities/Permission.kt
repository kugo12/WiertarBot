package pl.kvgx12.wiertarbot.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Permission(
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    val id: Int? = null,

    @Column(unique = true, nullable = false)
    val command: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    val whitelist: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    val blacklist: String,
)