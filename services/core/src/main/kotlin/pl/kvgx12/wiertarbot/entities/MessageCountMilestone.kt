package pl.kvgx12.wiertarbot.entities

import jakarta.persistence.*

@Entity(name = "messagecountmilestone")
class MessageCountMilestone(
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    val id: Int? = null,

    @Column(nullable = false, unique = true)
    val threadId: String,
    var count: Int,
)
