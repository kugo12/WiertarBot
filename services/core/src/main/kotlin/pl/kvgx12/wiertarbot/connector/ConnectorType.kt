package pl.kvgx12.wiertarbot.connector

import java.util.*

enum class ConnectorType {
    FB, Telegram;

    fun set(): EnumSet<ConnectorType> = EnumSet.of(this)

    companion object {
        fun all(): EnumSet<ConnectorType> = EnumSet.allOf(ConnectorType::class.java)
    }
}
