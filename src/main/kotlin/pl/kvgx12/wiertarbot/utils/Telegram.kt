@file:OptIn(RiskFeature::class)

package pl.kvgx12.wiertarbot.utils

import dev.inmo.micro_utils.coroutines.*
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.bot.exceptions.CommonBotException
import dev.inmo.tgbotapi.bot.exceptions.GetUpdatesConflict
import dev.inmo.tgbotapi.bot.exceptions.RequestException
import dev.inmo.tgbotapi.requests.GetUpdates
import dev.inmo.tgbotapi.requests.webhook.DeleteWebhook
import dev.inmo.tgbotapi.types.*
import dev.inmo.tgbotapi.types.message.abstracts.*
import dev.inmo.tgbotapi.types.message.content.MediaGroupContent
import dev.inmo.tgbotapi.types.message.content.MediaGroupPartContent
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.update.abstracts.BaseMessageUpdate
import dev.inmo.tgbotapi.types.update.abstracts.BaseSentMessageUpdate
import dev.inmo.tgbotapi.types.update.abstracts.Update
import dev.inmo.tgbotapi.updateshandlers.FlowsUpdatesFilter
import dev.inmo.tgbotapi.updateshandlers.UpdateReceiver
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.extensions.accumulateByKey
import dev.inmo.tgbotapi.utils.extensions.asMediaGroupMessage
import io.ktor.client.plugins.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

/**
 * Will convert incoming list of updates to list with [MediaGroupUpdate]s
 */
private fun List<Update>.convertWithMediaGroupUpdates(): List<Update> {
    val resultUpdates = mutableListOf<Update>()
    val mediaGroups =
        mutableMapOf<MediaGroupIdentifier, MutableList<Pair<BaseSentMessageUpdate, PossiblySentViaBotCommonMessage<MediaGroupPartContent>>>>()

    for (update in this) {
        val message = (update.data as? PossiblySentViaBotCommonMessage<*>)?.let {
            if (it.content is MediaGroupPartContent) {
                it as PossiblySentViaBotCommonMessage<MediaGroupPartContent>
            } else {
                null
            }
        }
        val mediaGroupId = message?.mediaGroupId
        if (message == null || mediaGroupId == null) {
            resultUpdates.add(update)
            continue
        }
        when (update) {
            is BaseSentMessageUpdate -> {
                mediaGroups.getOrPut(mediaGroupId) {
                    mutableListOf()
                }.add(update to message)
            }

            else -> resultUpdates.add(update)
        }
    }

    mediaGroups.map { (_, updatesWithMessages) ->
        val update = updatesWithMessages.maxBy { it.first.updateId }.first
        resultUpdates.add(
            update.copy(updatesWithMessages.map { it.second }.asMediaGroupMessage())
        )
    }

    resultUpdates.sortBy { it.updateId }
    return resultUpdates
}

private fun CoroutineScope.updateHandlerWithMediaGroupsAdaptation(
    output: UpdateReceiver<Update>,
    mediaGroupsDebounceMillis: Long = 1000L
): UpdateReceiver<Update> {
    val updatesChannel = Channel<Update>(Channel.UNLIMITED)
    val mediaGroupChannel = Channel<Pair<String, BaseMessageUpdate>>(Channel.UNLIMITED)
    val mediaGroupAccumulatedChannel = mediaGroupChannel.accumulateByKey(
        mediaGroupsDebounceMillis,
        scope = this
    )

    launch {
        launchSafelyWithoutExceptions {
            for (update in updatesChannel) {
                val data = update.data
                when {
                    data is PossiblyMediaGroupMessage<*> && data.mediaGroupId != null -> {
                        mediaGroupChannel.send("${data.mediaGroupId}${update::class.simpleName}" to update as BaseMessageUpdate)
                    }

                    else -> output(update)
                }
            }
        }
        launchSafelyWithoutExceptions {
            for ((_, mediaGroup) in mediaGroupAccumulatedChannel) {
                mediaGroup.convertWithMediaGroupUpdates().forEach {
                    output(it)
                }
            }
        }
    }

    return { updatesChannel.send(it) }
}

fun TelegramBot.longPollingFlow(
    timeoutSeconds: Seconds = 30,
    exceptionsHandler: (ExceptionHandler<Unit>)? = null,
    allowedUpdates: List<String>? = dev.inmo.tgbotapi.types.ALL_UPDATES_LIST,
    autoDisableWebhooks: Boolean = true,
    autoSkipTimeoutExceptions: Boolean = true,
    mediaGroupsDebounceTimeMillis: Long? = 1000L,
): Flow<Update> = channelFlow {
    if (autoDisableWebhooks) {
        runCatchingSafely {
            execute(DeleteWebhook())
        }
    }

    val contextSafelyExceptionHandler = coroutineContext[ContextSafelyExceptionHandlerKey]
    val contextToWork = if (contextSafelyExceptionHandler == null || !autoSkipTimeoutExceptions) {
        coroutineContext
    } else {
        coroutineContext + ContextSafelyExceptionHandler { e ->
            if (e is HttpRequestTimeoutException || (e is CommonBotException && e.cause is HttpRequestTimeoutException)) {
                return@ContextSafelyExceptionHandler
            } else {
                contextSafelyExceptionHandler.handler(e)
            }
        }
    }

    var lastUpdateIdentifier: UpdateIdentifier? = null

    val updatesHandler: (suspend (List<Update>) -> Unit) = if (mediaGroupsDebounceTimeMillis != null) {
        val scope = CoroutineScope(contextToWork)
        val updatesReceiver = scope.updateHandlerWithMediaGroupsAdaptation(
            {
                withContext(contextToWork) {
                    send(it)
                }
            },
            mediaGroupsDebounceTimeMillis
        );
        { originalUpdates: List<Update> ->
            originalUpdates.forEach {
                updatesReceiver(it)
                lastUpdateIdentifier = maxOf(lastUpdateIdentifier ?: it.updateId, it.updateId)
            }
        }
    } else {
        { originalUpdates: List<Update> ->
            val converted = originalUpdates.convertWithMediaGroupUpdates()

            /**
             * Dirty hack for cases when the media group was retrieved not fully:
             *
             * We are throw out the last media group and will reretrieve it again in the next get updates
             * and it will guarantee that it is full
             */
            /**
             * Dirty hack for cases when the media group was retrieved not fully:
             *
             * We are throw out the last media group and will reretrieve it again in the next get updates
             * and it will guarantee that it is full
             */
            val updates = if (
                originalUpdates.size == dev.inmo.tgbotapi.types.getUpdatesLimit.last
                && ((converted.last() as? BaseSentMessageUpdate)?.data as? CommonMessage<*>)?.content is MediaGroupContent<*>
            ) {
                converted - converted.last()
            } else {
                converted
            }

            safelyWithResult {
                for (update in updates) {
                    send(update)

                    lastUpdateIdentifier = update.updateId
                }
            }.onFailure {
                cancel(it as? CancellationException ?: return@onFailure)
            }
        }
    }

    withContext(contextToWork) {
        while (isActive) {
            safely(
                { e ->
                    val isHttpRequestTimeoutException =
                        e is HttpRequestTimeoutException || (e is CommonBotException && e.cause is HttpRequestTimeoutException)
                    if (isHttpRequestTimeoutException && autoSkipTimeoutExceptions) {
                        return@safely
                    }
                    exceptionsHandler?.invoke(e)
                    if (e is RequestException) {
                        delay(1000L)
                    }
                    if (e is GetUpdatesConflict && (exceptionsHandler == null || exceptionsHandler == defaultSafelyExceptionHandler)) {
                        println("Warning!!! Other bot with the same bot token requests updates with getUpdate in parallel")
                    }
                }
            ) {
                execute(
                    GetUpdates(
                        offset = lastUpdateIdentifier?.plus(1),
                        timeout = timeoutSeconds,
                        allowed_updates = allowedUpdates
                    )
                ).let { originalUpdates ->
                    updatesHandler(originalUpdates)
                }
            }
        }
    }
}


/**
 * Will enable [longPolling] by creating [FlowsUpdatesFilter] with [flowsUpdatesFilterUpdatesKeeperCount] as an argument
 * and applied [flowUpdatesPreset]. It is assumed that you WILL CONFIGURE all updates receivers in [flowUpdatesPreset],
 * because of after [flowUpdatesPreset] method calling will be triggered getting of updates.
 *
 * @param mediaGroupsDebounceTimeMillis Will be used for calling of [updateHandlerWithMediaGroupsAdaptation]. Pass null
 * in case you wish to enable classic way of updates handling, but in that mode some media group messages can be
 * retrieved in different updates
 */
fun TelegramBot.longPolling(
    timeoutSeconds: Seconds = 30,
    scope: CoroutineScope = CoroutineScope(Default),
    exceptionsHandler: ExceptionHandler<Unit>? = null,
    flowsUpdatesFilterUpdatesKeeperCount: Int = 100,
    autoDisableWebhooks: Boolean = true,
    autoSkipTimeoutExceptions: Boolean = true,
    mediaGroupsDebounceTimeMillis: Long? = 1000L,
    flowUpdatesPreset: FlowsUpdatesFilter.() -> Unit
): Job = FlowsUpdatesFilter(flowsUpdatesFilterUpdatesKeeperCount).run {
    flowUpdatesPreset()

    longPollingFlow(
        timeoutSeconds = timeoutSeconds,
        exceptionsHandler = exceptionsHandler,
        allowedUpdates = allowedUpdates,
        autoDisableWebhooks = autoDisableWebhooks,
        autoSkipTimeoutExceptions = autoSkipTimeoutExceptions,
        mediaGroupsDebounceTimeMillis = mediaGroupsDebounceTimeMillis
    ).subscribeSafely(
        scope = scope,
        onException = exceptionsHandler ?: defaultSafelyExceptionHandler,
        block = asUpdateReceiver
    )
}

inline val Message.text get() = tryCast<ContentMessage<*>>()?.content?.tryCast<TextContent>()?.text
