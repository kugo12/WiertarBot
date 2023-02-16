package pl.kvgx12.wiertarbot.python

import jep.python.PyObject

interface Inspect {
    fun isfunction(obj: PyObject): Boolean
    fun iscoroutinefunction(obj: PyObject): Boolean
    fun isawaitable(obj: PyObject): Boolean
    fun isclass(obj: PyObject): Boolean
}