package pl.kvgx12.wiertarbot.utils.proto

import pl.kvgx12.wiertarbot.proto.ConnectorType
import java.util.*

fun ConnectorType.set(): EnumSet<ConnectorType> = EnumSet.of(this)
