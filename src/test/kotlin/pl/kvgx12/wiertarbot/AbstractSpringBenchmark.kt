package pl.kvgx12.wiertarbot

import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.TearDown
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext

abstract class AbstractSpringBenchmark {
    private var context: ConfigurableApplicationContext? = null

    @Setup
    fun setupBenchmark() {
        context = runApplication<CoreApplication>().apply { setup() }
    }

    @TearDown
    fun tearDownBenchmark() {
        tearDown()
        context!!.close()
        context = null
    }

    abstract fun ConfigurableApplicationContext.setup()
    abstract fun tearDown()
}