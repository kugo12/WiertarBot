package pl.kvgx12.wiertarbot.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T> T.getLogger(): Logger = when {
    T::class.isCompanion -> T::class.java.declaringClass
    else -> T::class.java
}.let(LoggerFactory::getLogger)

fun Logger.error(throwable: Throwable) = error(throwable.message, throwable)
