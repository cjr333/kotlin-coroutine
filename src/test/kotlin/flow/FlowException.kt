package flow

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class FlowException {
    @Test
    fun `using try-catch`() {
        fun simple() = flow {
            for (i in 1..3) {
                log("Emitting $i")
                emit(i) // emit next value
            }
        }.map { value ->
            check(value <= 1) { "Crashed on $value" }
            "string $value"
        }

        runBlocking {
            try {
                simple().collect { value -> log(value) }
            } catch (e: Throwable) {
                log("Caught $e")
            }
        }
    }

    @Test
    fun `using catch of Flow`() {
        fun simple() = flow {
            for (i in 1..3) {
                log("Emitting $i")
                emit(i)
            }
        }

        runBlocking {
            simple()
                .catch { e -> log("Caught $e") } // does not catch downstream exceptions
                .collect { value ->
                    check(value <= 1) { "Collected $value" }
                    log(value)
                }
        }
    }

    @Test
    fun `catching declaratively`() {
        // moving the body of the collect operator into onEach and putting it before the catch operator.
        fun simple() = flow {
            for (i in 1..3) {
                log("Emitting $i")
                emit(i)
            }
        }

        runBlocking {
            simple()
                .onEach { value ->
                    check(value <= 1) { "Collected $value" }
                    log(value)
                }.catch { e -> log("Caught $e") } // does not catch downstream exceptions
                .collect {}
        }
    }
}