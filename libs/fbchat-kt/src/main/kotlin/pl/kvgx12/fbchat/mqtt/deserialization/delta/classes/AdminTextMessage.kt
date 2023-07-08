package pl.kvgx12.fbchat.mqtt.deserialization.delta.classes

import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import pl.kvgx12.fbchat.data.UserId
import pl.kvgx12.fbchat.data.events.ThreadEvent
import pl.kvgx12.fbchat.utils.surrogateDeserializer

@Serializable
private data class AdminMessage<T>(
    val messageMetadata: NewMessageDelta.MessageMetadata,
    val untypedData: T,
) {
    inline val author get() = UserId(messageMetadata.actorFbId)
    inline val thread get() = messageMetadata.threadKey.toThreadId()
    inline val timestamp get() = messageMetadata.timestamp.toLong()
}

@Serializable
private data class ChangeThreadTheme(
    @SerialName("theme_color")
    val themeColor: String,
)

internal val changeThreadThemeDeserializer = surrogateDeserializer<AdminMessage<ChangeThreadTheme>, _> {
    flowOf(
        ThreadEvent.ColorSet(
            author = it.author,
            thread = it.thread,
            timestamp = it.timestamp,
            color = it.untypedData.themeColor,
        ),
    )
}

@Serializable
private data class ChangeThreadIcon(
    @SerialName("thread_icon")
    val threadIcon: String,
)

internal val changeThreadIconDeserializer = surrogateDeserializer<AdminMessage<ChangeThreadIcon>, _> {
    flowOf(
        ThreadEvent.EmojiSet(
            thread = it.thread,
            author = it.author,
            emoji = it.untypedData.threadIcon,
            timestamp = it.timestamp,
        ),
    )
}

@Serializable
private data class ChangeThreadNickname(
    @SerialName("participant_id")
    val participantId: String,
    val nickname: String?,
)

internal val changeThreadNicknameDeserializer = surrogateDeserializer<AdminMessage<ChangeThreadNickname>, _> {
    flowOf(
        ThreadEvent.NicknameSet(
            thread = it.thread,
            author = it.author,
            subject = UserId(it.untypedData.participantId),
            nickname = it.untypedData.nickname?.ifEmpty { null },
            timestamp = it.timestamp,
        ),
    )
}

@Serializable
private data class ChangeThreadAdmins(
    @SerialName("ADMIN_EVENT")
    val adminEvent: String,
    @SerialName("TARGET_ID")
    val targetId: String,
)

internal val changeThreadAdminsDeserializer = surrogateDeserializer<AdminMessage<ChangeThreadAdmins>, _> {
    val event = when (it.untypedData.adminEvent) {
        "add_admin" -> ThreadEvent.AdminsAdded(
            author = it.author,
            thread = it.thread,
            timestamp = it.timestamp,
            added = listOf(UserId(it.untypedData.targetId)),
        )

        "remove_admin" -> ThreadEvent.AdminsRemoved(
            author = it.author,
            thread = it.thread,
            timestamp = it.timestamp,
            removed = listOf(UserId(it.untypedData.targetId)),
        )

        else -> throw SerializationException("Invalid adminEvent: ${it.untypedData.adminEvent}")
    }

    flowOf(event)
}

@Serializable
private data class ChangeThreadApprovalMode(
    @SerialName("APPROVAL_MODE")
    val approvalMode: String,
)

internal val changeThreadApprovalModeDeserializer = surrogateDeserializer<AdminMessage<ChangeThreadApprovalMode>, _> {
    flowOf(
        ThreadEvent.ApprovalModeSet(
            author = it.author,
            thread = it.thread,
            timestamp = it.timestamp,
            requireAdminApproval = it.untypedData.approvalMode == "1",
        ),
    )
}

@Serializable
private data class CallLog(
    val event: String,
    @SerialName("call_duration")
    val callDuration: String? = null,
)

internal val callLogDeserializer = surrogateDeserializer<AdminMessage<CallLog>, _> {
    val event = when (it.untypedData.event) {
        "group_call_started" -> ThreadEvent.CallStarted(
            author = it.author,
            thread = it.thread,
            timestamp = it.timestamp,
        )

        "group_call_ended", "one_on_one_call_ended" -> ThreadEvent.CallFinished(
            author = it.author,
            thread = it.thread,
            timestamp = it.timestamp,
            duration = it.untypedData.callDuration!!.toLong(),
        )

        else -> throw SerializationException("Unknown call event type: ${it.untypedData.event}")
    }

    flowOf(event)
}

internal val participantJoinedGroupCallDeserializer = surrogateDeserializer<AdminMessage<Unit?>, _> {
    flowOf(
        ThreadEvent.CallJoined(
            thread = it.thread,
            timestamp = it.timestamp,
            author = it.author,
        ),
    )
}

@Serializable
private data class ChangeThreadQuickReaction(
    @SerialName("thread_quick_reaction_emoji")
    val themeEmoji: String,
)

internal val changeThreadQuickReactionDeserializer = surrogateDeserializer<AdminMessage<ChangeThreadQuickReaction>, _> {
    flowOf(
        ThreadEvent.EmojiSet(
            thread = it.thread,
            author = it.author,
            emoji = it.untypedData.themeEmoji,
            timestamp = it.timestamp,
        ),
    )
}
