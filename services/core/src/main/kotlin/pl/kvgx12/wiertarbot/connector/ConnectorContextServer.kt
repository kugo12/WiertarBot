package pl.kvgx12.wiertarbot.connector

import pl.kvgx12.wiertarbot.proto.ConnectorType
import pl.kvgx12.wiertarbot.proto.connector.ConnectorContextGrpcKt

abstract class ConnectorContextServer(val connectorType: ConnectorType) : ConnectorContextGrpcKt.ConnectorContextCoroutineImplBase()
