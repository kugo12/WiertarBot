package pl.kvgx12.wiertarbot.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity(name = "fbmessage")
class FBMessage(
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    val id: Int? = null,

    @Column(nullable = false, unique = true)
    val messageId: String,
    val threadId: String,
    val authorId: String,
    val time: Long,
    @Column(columnDefinition = "TEXT", nullable = false)
    val message: String,
    var deletedAt: Long? = null,
)