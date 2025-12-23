package pl.kvgx12.telegram.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TResult<T : Any>(
    val ok: Boolean,
    val description: String? = null,
    @SerialName("error_code")
    val errorCode: Int? = null,
    val result: T? = null,
)
