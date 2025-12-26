package pl.kvgx12.telegram.data.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.kvgx12.telegram.data.TChatAdministratorRights

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#setmydefaultadministratorrights)
 */
@Serializable
data class TSetMyDefaultAdministratorRightsRequest(
    val rights: @Serializable(TChatAdministratorRights.Serializer::class) TChatAdministratorRights? = null,
    @SerialName("for_channels")
    val forChannels: Boolean? = null,
)
