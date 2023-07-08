package pl.kvgx12.fbchat.mqtt.deserialization.delta

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import pl.kvgx12.fbchat.data.*
import pl.kvgx12.fbchat.mqtt.deserialization.delta.classes.*
import pl.kvgx12.fbchat.mqtt.deserialization.delta.payload.clientPayloadDeserializer
import pl.kvgx12.fbchat.utils.surrogateDeserializer
import pl.kvgx12.fbchat.utils.tryAsString
import kotlin.reflect.KClass
import pl.kvgx12.fbchat.data.events.Event as FBEvent

internal typealias Delta =
    @Serializable(DeltaSerializer::class)
    Flow<FBEvent>

@Suppress("UNCHECKED_CAST")
internal object DeltaSerializer : JsonContentPolymorphicSerializer<Delta>(Flow::class as KClass<Flow<FBEvent>>) {
    val emptyFlowDeserializer = surrogateDeserializer<Unit, Delta> { emptyFlow() }

    val unknownDeltaDeserializer = surrogateDeserializer<JsonElement, _> {
        flowOf(FBEvent.Unknown("/t_ms", it))
    }

    private val serializers = mapOf<String?, _>(
        "ParticipantsAddedToGroupThread" to participantsAddedToGroupThreadDeserializer,
        "ParticipantLeftGroupThread" to participantLeftGroupThreadDeserializer,
        "MarkFolderSeen" to markFolderSeenDeserializer,
        "ThreadName" to threadNameDeserializer,
        "ForcedFetch" to forcedFetchDeserializer,
        "DeliveryReceipt" to deliveryReceiptDeserializer,
        "ReadReceipt" to readReceiptDeserializer,
        "MarkRead" to markReadDeserializer,
        "NoOp" to emptyFlowDeserializer,
        "NewMessage" to newMessageDeltaDeserializer,
        "ThreadFolder" to threadFolderDeserializer,
        "ClientPayload" to clientPayloadDeserializer,
    )

    private val adminMessageSerializers = mapOf<String?, _>(
        "change_thread_theme" to changeThreadThemeDeserializer,
        "change_thread_icon" to changeThreadIconDeserializer,
        "change_thread_nickname" to changeThreadNicknameDeserializer,
        "change_thread_admins" to changeThreadAdminsDeserializer,
        "change_thread_approval_mode" to changeThreadApprovalModeDeserializer,
        "change_thread_quick_reaction" to changeThreadQuickReactionDeserializer,
        "messenger_call_log" to callLogDeserializer,
        "participant_joined_group_call" to participantJoinedGroupCallDeserializer,
    )

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Delta> {
        require(element is JsonObject)

        val klass = element["class"].tryAsString()

        return if (klass == "AdminTextMessage") {
            adminMessageSerializers.getOrDefault(
                element["type"].tryAsString(),
                unknownDeltaDeserializer,
            )
        } else {
            serializers.getOrDefault(klass, unknownDeltaDeserializer)
        }
    }
}
