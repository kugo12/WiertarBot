package pl.kvgx12.wiertarbot.connector

import java.util.*

enum class ConnectorType {
    FB, Telegram;

    fun set() = EnumSet.of(this)

    companion object {
        fun all() = EnumSet.allOf(ConnectorType::class.java)
    }
}
