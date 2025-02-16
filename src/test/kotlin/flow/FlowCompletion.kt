package flow

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class FlowCompletion {
    @Test
    fun `imperatively finally block`() {
        fun simple() = (1..3).asFlow()
        runBlocking {
            try {
                simple().collect { value -> log(value) }
            } finally {
                log("Done")
            }
        }
    }

    @Test
    fun `declaratively handling`() {
        fun simple() = (1..3).asFlow()
        runBlocking {
            simple()
                .onCompletion { log("Done") }
                .collect { value -> log(value) }
        }
    }

    @Test
    fun `completed exceptionally`() {
        fun simple() = flow {
            emit(1)
            throw RuntimeException()
        }
        runBlocking {
            simple()
                .onCompletion { cause -> if (cause != null) log("Flow completed exceptionally") }
                .catch { cause -> log("Caught exception") }
                .collect { value -> log(value) }
        }
    }

    @Test
    fun `multiple completion handler`() {
        fun simple() = flow {
            emit(1)
        }
        runBlocking {
            simple()
                .onCompletion { log("First Done") }
                .onCompletion { log("Second Done") }
                .collect { value -> log(value) }
        }

        runBlocking {
            simple()
                .onCompletion { cause -> if (cause != null) log("First Exception - ${cause.message}") }
                .onCompletion { cause -> if (cause != null) log("Second Exception - ${cause.message}") }
                .map { it / 0 }
                .catch { cause -> log("Caught exception") }
                .onCompletion { cause -> if (cause != null) log("Third Exception - ${cause.message}") }
                .collect { value -> log(value) }
        }
    }
}