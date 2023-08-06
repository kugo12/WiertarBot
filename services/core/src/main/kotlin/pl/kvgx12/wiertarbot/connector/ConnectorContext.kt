package pl.kvgx12.wiertarbot.connector

import pl.kvgx12.wiertarbot.proto.ConnectorType
import pl.kvgx12.wiertarbot.proto.connector.*

abstract class ConnectorContext(
    val connectorType: ConnectorType,
) : ConnectorContextGrpcKt.ConnectorContextCoroutineImplBase() {
    suspend inline fun upload(file: String, voiceClip: Boolean = false) = upload(
        uploadRequest {
            files += file
            this.voiceClip = voiceClip
        },
    )
}
