package flow

import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException
import kotlin.system.measureTimeMillis
import kotlin.test.Test

class Buffering {
    @Test
    fun sequential() {
        fun simple() = flow {
            for (i in 1..3) {
                delay(100) // pretend we are asynchronously waiting 100 ms
                emit(i) // emit next value
            }
        }

        runBlocking {
            val time = measureTimeMillis {
                simple().collect { value ->
                    delay(300) // pretend we are processing it for 300 ms
                    log(value)
                }
            }
            log("Collected in $time ms")
        }
    }

    @Test
    fun buffer() {
        fun simple() = flow {
            for (i in 1..3) {
                delay(100) // pretend we are asynchronously waiting 100 ms
                emit(i) // emit next value
            }
        }

        runBlocking {
            val time = measureTimeMillis {
                simple()
                    .buffer()
                    .collect { value ->
                        delay(300) // pretend we are processing it for 300 ms
                        log(value)
                    }
            }
            log("Collected in $time ms")
        }
    }

    @Test
    fun conflate() {
        fun simple() = flow {
            for (i in 1..3) {
                delay(100) // pretend we are asynchronously waiting 100 ms
                emit(i) // emit next value
            }
        }

        runBlocking {
            val time = measureTimeMillis {
                simple()
                    .conflate()
                    .collect { value ->
                        delay(300) // pretend we are processing it for 300 ms
                        log(value)
                    }
            }
            log("Collected in $time ms")
        }

        runBlocking {
            val time = measureTimeMillis {
                simple()
                    .buffer(CONFLATED)
                    .collect { value ->
                        delay(300) // pretend we are processing it for 300 ms
                        log(value)
                    }
            }
            log("Collected in $time ms")
        }
    }

    @Test
    fun collectLatest() {
        fun simple() = flow {
            for (i in 1..3) {
                log("Start emitting $i")
                delay(100) // pretend we are asynchronously waiting 100 ms
                emit(i) // emit next value
            }
        }

        runBlocking {
            val time = measureTimeMillis {
                simple()
                    .collectLatest { value ->
                        try {
                            // cancel & restart on the latest value
                            log("Collecting $value")
                            delay(300) // pretend we are processing it for 300 ms
                            log("Done $value")
                        } catch(ex: CancellationException) {
                            log("Canceled. $ex")
                        }
                    }
            }
            log("Collected in $time ms")
        }
    }
}