package pl.kvgx12.wiertarbot.entities

import jakarta.persistence.*

@Entity
class Permission(
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    val id: Int? = null,

    @Column(unique = true, nullable = false)
    val command: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    var whitelist: String,

    @Column(columnDefinition = "TEXT", nullable = false)
    var blacklist: String,
)
