@file:Suppress("FunctionName", "NOTHING_TO_INLINE")

package pl.kvgx12.wiertarbot.python

import jep.Jep
import jep.python.PyObject

fun interface PyCallable0<R> {
    fun __call__(): R
}

fun interface PyCallable1<T, R> {
    fun __call__(arg0: T): R
}

fun interface PyCallable2<A0, A1, R> {
    fun __call__(arg0: A0, arg1: A1): R
}

inline fun <reified T> Jep.getProxiedValue(name: String): T = getValue(name, PyObject::class.java).proxy()
inline fun <reified T> PyObject.proxy(): T = proxy(T::class.java)
inline fun <reified T> PyObject.get(name: String): T = getAttr(name, T::class.java)
inline fun PyObject.pyGet(name: String): PyObject = get(name)
inline fun PyObject.className(): String = pyGet("__class__").get("__name__")
