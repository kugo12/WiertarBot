package pl.kvgx12.wiertarbot.connector

import pl.kvgx12.wiertarbot.proto.ConnectorType
import pl.kvgx12.wiertarbot.proto.connector.ConnectorContextGrpcKt
import pl.kvgx12.wiertarbot.proto.connector.DelegatedCommandRequest
import pl.kvgx12.wiertarbot.proto.connector.Empty

abstract class ConnectorContextServer(
    val connectorType: ConnectorType,
    val delegatedCommand: DelegatedCommandInvoker
) : ConnectorContextGrpcKt.ConnectorContextCoroutineImplBase() {
    final override suspend fun delegatedCommand(request: DelegatedCommandRequest): Empty {
        delegatedCommand(request)
        return Empty.getDefaultInstance()
    }
}
