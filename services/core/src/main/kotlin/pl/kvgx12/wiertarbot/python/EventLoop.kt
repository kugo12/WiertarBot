@file:Suppress("FunctionName")

package pl.kvgx12.wiertarbot.python

import jep.python.PyObject

interface EventLoop {
    fun call_soon_threadsafe(callable: PyObject)
    fun call_soon_threadsafe(callable: PyObject, arg0: Any?)
    fun call_soon_threadsafe(callable: PyObject, arg0: Any?, arg1: Any?)
    fun call_soon_threadsafe(callable: PyCallable0<*>)
    fun <T> call_soon_threadsafe(callable: PyCallable1<T, *>, arg0: T)
    fun <T, V> call_soon_threadsafe(callable: PyCallable2<T, V, *>, arg0: T, arg1: V)
}
