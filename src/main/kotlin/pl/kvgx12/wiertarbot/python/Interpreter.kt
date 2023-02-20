package pl.kvgx12.wiertarbot.python

import jakarta.annotation.PreDestroy
import jep.JepConfig
import jep.SharedInterpreter
import jep.python.PyCallable
import jep.python.PyObject
import kotlinx.coroutines.*
import org.intellij.lang.annotations.Language
import pl.kvgx12.wiertarbot.execute
import pl.kvgx12.wiertarbot.utils.getLogger
import pl.kvgx12.wiertarbot.utils.newSingleThreadDispatcher
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Language("python")
private const val STARTUP_SCRIPT = """
import asyncio, threading, inspect

loop: asyncio.AbstractEventLoop = asyncio.new_event_loop()

def __run_coroutine(fun, callback):
    fut = asyncio.ensure_future(fun, loop=loop)
    fut.add_done_callback(lambda _: callback(fut.result()))

def __add_task(coro, fut: asyncio.Future):
    task = loop.create_task(coro)
    fut.set_result(task)

def start_coroutine(fun, callback):
    loop.call_soon_threadsafe(__run_coroutine, fun, callback)

def add_task(coro) -> asyncio.Task:
    fut = asyncio.Future(loop=loop)
    loop.call_soon_threadsafe(__add_task, coro, fut)

    while not fut.done():
        pass
    return fut.result()

def launch_task(coro) -> None:
    loop.call_soon_threadsafe(loop.create_task, coro)

def __setup_globals():
    global wbglobals, loop
    import types, sys
    
    wbglobals["loop"] = loop
    sys.modules['wbglobals'] = types.ModuleType('wbglobals')
    for it in wbglobals:
        sys.modules['wbglobals'].__dict__[it] = wbglobals[it]
        
    del wbglobals

__setup_globals()
import WiertarBot
"""

@Language("python")
private const val ASYNC_SCRIPT = """
import wbglobals, asyncio
loop = wbglobals.loop

asyncio.set_event_loop(loop)
try:
    loop.run_forever()
finally:
    loop.close()
"""

class Interpreter(
    config: JepConfig,
    val context: ExecutorCoroutineDispatcher,
    globals: Map<String, Any>,
): SharedInterpreter() {
    private val log = getLogger()
    init {
        configureInterpreter(config)
        set("wbglobals", globals.toMutableMap().apply {
            put("log", log)
        })
        execute(STARTUP_SCRIPT)
    }

    private val asyncThread = SharedAsyncInterpreter()
    private val pyFutureScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val contextScope = CoroutineScope(context + SupervisorJob())

    val startCoroutine: PyCallable = get("start_coroutine")
    val addTask: PyCallable = get("add_task")
    val launchTask: PyCallable = get("launch_task")
    val wiertarBot: WiertarBotModule = getProxiedValue("WiertarBot")
    val inspect: Inspect = getProxiedValue("inspect")

    @PreDestroy
    private fun preDestroy() {
        runBlocking(context) {
            execute("""loop.call_soon_threadsafe(loop.stop)""".trimIndent())
            asyncThread.eventLoopJob.cancel()
            asyncThread.context {
                asyncThread.interpreter.close()
            }
            asyncThread.context.cancel()
            pyFutureScope.cancel()
            close()
        }
    }

    suspend inline operator fun<T> invoke(crossinline f: suspend Interpreter.() -> T) =
        context { f() }

    inline fun launch(crossinline f: suspend Interpreter.() -> Unit) =
        contextScope.launch { f() }

    suspend inline fun PyCallable.intoCoroutine(
        args: Array<Any> = emptyArray(),
        kwargs: Map<String, Any> = emptyMap(),
    ): Any? = context {
        suspendCoroutine { continuation ->
            startCoroutine.call(
                call(args, kwargs),
                PyCallable1<Any?, Unit> { continuation.resume(it) }
            )
        }
    }

    suspend inline fun PyObject.pyAwait() = context {
        suspendCoroutine { continuation ->
            startCoroutine.call(
                this@pyAwait,
                PyCallable1<Any?, Unit> { continuation.resume(it) }
            )
        }
    }

    fun<T> createFuture(): PyFuture<T> = runBlocking(context) {
        getProxiedValue("asyncio.Future(loop=loop)")
    }

    fun<T> wrapIntoFuture(f: suspend () -> T): PyFuture<T> {
        val future = createFuture<T>()

        pyFutureScope.launch {
            val result = kotlin.runCatching { f() }

            context {
                result.fold(future::set_result, future::set_exception)
            }
        }.invokeOnCompletion {
            if (it is CancellationException)
                runBlocking(context) {
                    future.cancel(it)
                }
        }

        return future
    }

    inline fun<reified T> get(name: String) = getValue(name, T::class.java)

    class SharedAsyncInterpreter {
        val context = newSingleThreadDispatcher("asyncio-loop")
        val interpreter = runBlocking(context) { SharedInterpreter() }
        val eventLoopJob = CoroutineScope(context).launch {
            interpreter.execute(ASYNC_SCRIPT.trimIndent())
        }
    }
}
