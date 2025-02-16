package flow

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class SequenceAndFlow {
    @Test
    fun sequenceTest() {
        fun simple(): Sequence<Int> = sequence { // sequence builder
            for (i in 1..3) {
                Thread.sleep(100) // pretend we are computing it
                yield(i) // yield next value
            }
        }

        simple().forEach { value -> log(value) }
    }

    @Test
    fun suspendTest() {
        suspend fun simple(): List<Int> {
            delay(1000) // pretend we are doing something asynchronous here
            return listOf(1, 2, 3)
        }

        runBlocking {
            simple().forEach { value -> log(value) }
        }
    }

    @Test
    fun flowTest() {
        fun simple() = flow { // flow builder
            for (i in 1..3) {
                delay(100) // pretend we are doing something useful here
                emit(i) // emit next value
            }
        }

        runBlocking {
            // Launch a concurrent coroutine to check if the main thread is blocked
            launch {
                for (k in 1..3) {
                    log("I'm not blocked $k")
                    delay(100)
                }
            }
            // Collect the flow
            simple().collect { value -> log(value) }
        }
    }
}