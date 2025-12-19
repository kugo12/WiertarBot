@file:Suppress("NOTHING_TO_INLINE")

package pl.kvgx12.wiertarbot.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

const val KiB = 1024

annotation class AllOpen

inline fun <reified T> T.getLogger(): Logger = when {
    T::class.isCompanion -> T::class.java.declaringClass
    else -> T::class.java
}.let(LoggerFactory::getLogger)

fun Logger.error(throwable: Throwable) = error(throwable.message, throwable)

suspend fun Path.contentType(): String? = withContext(Dispatchers.IO) { Files.probeContentType(this@contentType) }
