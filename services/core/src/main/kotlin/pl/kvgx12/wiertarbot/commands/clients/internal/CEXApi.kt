package pl.kvgx12.wiertarbot.commands.clients.internal

import kotlinx.serialization.Serializable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.PostExchange
import pl.kvgx12.wiertarbot.config.properties.CEXProperties

interface CEXApi {
    @PostExchange("/api/execute")
    suspend fun execute(@RequestBody request: ExecuteRequest): ExecuteResponse

    @Serializable
    data class ExecuteRequest(
        val apiKey: String,
        val code: String,
        val timeout: Long,
    )

    @Serializable
    data class ExecuteResponse(
        val result: String,
        val error: Int,
    ) {
        val errorCode: ErrorCode
            get() = when (error) {
                0 -> ErrorCode.NONE
                1 -> ErrorCode.TIMEOUT
                2 -> ErrorCode.RUNTIME_ERROR
                else -> ErrorCode.RUNTIME_ERROR
            }
    }

    enum class ErrorCode {
        NONE,
        TIMEOUT,
        RUNTIME_ERROR,
    }
}

class CEXClient(
    private val cexApi: CEXApi,
    private val props: CEXProperties,
) {
    suspend fun executeCode(code: String): CEXApi.ExecuteResponse = cexApi.execute(
        CEXApi.ExecuteRequest(
            apiKey = props.apiKey,
            code = code,
            timeout = props.timeout,
        ),
    )
}
