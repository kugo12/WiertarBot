@file:Suppress("NOTHING_TO_INLINE")

package pl.kvgx12.wiertarbot.utils

import jep.Jep
import org.intellij.lang.annotations.Language

inline fun <T: Any> Any.tryCast(): T? = this as? T

inline fun Jep.execute(@Language("python") code: String) = exec(code)
