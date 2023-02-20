package pl.kvgx12.wiertarbot.events

import pl.kvgx12.wiertarbot.connector.ConnectorContext

sealed interface Event {
    val context: ConnectorContext?
}