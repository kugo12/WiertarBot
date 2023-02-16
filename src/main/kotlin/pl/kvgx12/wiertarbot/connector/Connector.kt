package pl.kvgx12.wiertarbot.connector

import kotlinx.coroutines.flow.Flow
import pl.kvgx12.wiertarbot.events.Event


interface Connector {
    fun run(): Flow<Event>
}