package pl.kvgx12.wiertarbot.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("messagecountmilestone")
data class MessageCountMilestone(
    @Id
    val id: Int? = null,
    val threadId: String,
    var count: Int,
)
