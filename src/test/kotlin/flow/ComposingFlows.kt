package flow

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class ComposingFlows {
    @Test
    fun zip() {
        val nums = (1..3).asFlow() // numbers 1..3
        val strs = flowOf("one", "two", "three") // strings
        runBlocking {
            nums.zip(strs) { a, b -> "$a -> $b" } // compose a single string
                .collect { log(it) } // collect and print
        }
    }

    @Test
    fun combine() {
        val nums = (1..3).asFlow().onEach { delay(300) } // numbers 1..3 every 300 ms
        val strs = flowOf("one", "two", "three").onEach { delay(400) } // strings every 400 ms

        runBlocking {
            log("On zip")
            var startTime = System.currentTimeMillis() // remember the start time
            nums.zip(strs) { a, b -> "$a -> $b" } // compose a single string with "zip"
                .collect { value -> // collect and print
                    log("$value at ${System.currentTimeMillis() - startTime} ms from start")
                }

            log("On combine")
            startTime = System.currentTimeMillis() // remember the start time
            nums.combine(strs) { a, b -> "$a -> $b" } // compose a single string with "zip"
                .collect { value -> // collect and print
                    log("$value at ${System.currentTimeMillis() - startTime} ms from start")
                }
        }
    }
}