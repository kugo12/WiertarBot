package pl.kvgx12.wiertarbot.utils

import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

fun newSingleThreadDispatcher(threadName: String) =
    Executors.newSingleThreadExecutor {
        Thread(it, threadName)
    }.asCoroutineDispatcher()
