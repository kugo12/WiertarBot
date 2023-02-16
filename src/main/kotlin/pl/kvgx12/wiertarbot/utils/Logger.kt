package pl.kvgx12.wiertarbot.utils

import org.slf4j.LoggerFactory

inline fun <reified T> T.getLogger() = when {
    T::class.isCompanion -> T::class.java.declaringClass
    else -> T::class.java
}.let(LoggerFactory::getLogger)
