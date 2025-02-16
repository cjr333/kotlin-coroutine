package flow

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import java.util.stream.Stream
import kotlin.streams.asStream
import kotlin.test.Test

class FlowIsCold {
    @Test
    fun `flow is cold`() {
        fun simple() = flow {
            log("Flow started")
            for (i in 1..3) {
                delay(100)
                emit(i)
            }
        }

        runBlocking {
            log("Calling simple function...")
            val flow = simple()
            log("Calling collect...")
            flow.collect { value -> log(value) }
            log("Calling collect again...")
            flow.collect { value -> log(value) }
        }
    }

    @Test
    fun `sequence is cold`() {
        fun simple(): Sequence<Int> = sequence { // seque
            log("Sequence started")// nce builder
            for (i in 1..3) {
                Thread.sleep(100) // pretend we are computing it
                yield(i) // yield next value
            }
        }

        log("Calling simple function...")
        val sequence = simple()
        log("Calling collect...")
        sequence.forEach { value -> log(value) }
        log("Calling collect again...")
        sequence.forEach { value -> log(value) }
    }

    @Test
    fun `stream is cold`() {
        fun simple(): Stream<Int> = sequence { // seque
            log("Stream started")// nce builder
            for (i in 1..3) {
                Thread.sleep(100) // pretend we are computing it
                yield(i) // yield next value
            }
        }.asStream()

        log("Calling simple function...")
        val sequence = simple()
        log("Calling collect...")
        sequence.forEach { value -> log(value) }
        log("Calling collect again...")
        sequence.forEach { value -> log(value) }
    }
}