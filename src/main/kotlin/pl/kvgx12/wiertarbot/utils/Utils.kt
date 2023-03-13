@file:Suppress("NOTHING_TO_INLINE")

package pl.kvgx12.wiertarbot.utils

inline fun <T: Any> Any.tryCast(): T? = this as? T
