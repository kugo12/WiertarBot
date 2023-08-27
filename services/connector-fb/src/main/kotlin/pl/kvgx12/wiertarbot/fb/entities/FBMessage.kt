package pl.kvgx12.wiertarbot.fb.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("fbmessage")
data class FBMessage(
    @Id
    val id: Int? = null,
    val messageId: String,
    val threadId: String,
    val authorId: String,
    val time: Long,
    val message: String,
    var deletedAt: Long? = null,
)
