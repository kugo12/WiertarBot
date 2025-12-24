package pl.kvgx12.telegram

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.shareIn
import org.slf4j.LoggerFactory
import pl.kvgx12.telegram.data.TUpdate
import pl.kvgx12.telegram.data.Update

class TelegramWebhook(
    val client: TelegramClient,
    val webhookUrl: String,
    private val secret: String,
    updateBufferSize: Int = 100
) {
    private val log = LoggerFactory.getLogger(TelegramWebhook::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val updates = Channel<Update>(updateBufferSize)
    private val updateFlow = updates.consumeAsFlow()
        .shareIn(scope, started = SharingStarted.Eagerly, replay = 0)

    @Suppress("TooGenericExceptionCaught")
    suspend fun initWebhook(): Boolean {
        log.info("Setting webhook to $webhookUrl")
        try {
            val result = client.setWebhook(
                url = webhookUrl,
                secretToken = secret,
                dropPendingUpdates = true,
            )
            log.info("Webhook set successfully: $result")

            return result
        } catch (e: Exception) {
            log.error("Failed to set webhook", e)
            throw e
        }
    }

    fun updates(): Flow<Update> = updateFlow

    @Suppress("TooGenericExceptionCaught")
    suspend fun close() {
        updates.close()
        scope.cancel("TelegramWebhook is closing")
        log.info("Closing webhook, removing webhook from Telegram")

        try {
            val result = client.deleteWebhook()
            log.info("Webhook removed successfully: $result")
        } catch (e: Exception) {
            log.error("Failed to remove webhook", e)
        }
    }

    suspend fun handleUpdate(getHeader: (String) -> String?, body: suspend () -> TUpdate) {
        if (getHeader(TELEGRAM_SECRET_HEADER) != secret) {
            log.warn("Received update with invalid secret")
            return
        }

        body().toUpdate()?.let {
            updates.send(it)
        }
    }

    companion object {
        private const val TELEGRAM_SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token"
    }
}
