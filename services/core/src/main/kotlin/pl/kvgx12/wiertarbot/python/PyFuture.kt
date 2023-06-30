package pl.kvgx12.wiertarbot.python

@Suppress("FunctionName")
interface PyFuture<T> {
    fun set_result(obj: T)
    fun set_exception(obj: Any?)
    fun cancel(msg: Any?)

    fun done(): Boolean
    fun cancelled(): Boolean
    fun result(): T
}
