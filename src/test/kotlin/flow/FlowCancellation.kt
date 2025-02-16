package flow

import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test

class FlowCancellation {
    @Test
    fun cancellation() {
        fun simple() = flow {
            for (i in 1..3) {
                delay(100)
                log("Emitting $i")
                emit(i)
            }
        }

        runBlocking {
            withTimeoutOrNull(250) { // Timeout after 250ms
                simple().collect { value -> log(value) }
            }
            log("Done")
        }
    }

    @Test
    fun `emit check cancellable`() {
        // https://github.com/Kotlin/kotlinx.coroutines/blob/00ae1a3318dbf7f6445270da47804216f70c39bb/kotlinx-coroutines-core/jvm/src/flow/internal/SafeCollector.kt#L105
        fun foo() = flow {
            for (i in 1..5) {
                log("Emitting $i")
                emit(i)
            }
        }
        runBlocking {
            foo().collect { value ->
                if (value == 3) cancel()
                log(value)
            }
        }
    }

    @Test
    fun `collect is not cancellable and make cancellable`() {
        runCatching {
            runBlocking {
                (1..5).asFlow().collect { value ->
                    if (value == 3) cancel()
                    log(value)
                }
            }
        }.onFailure {
            println(it)
        }

        runCatching {
            runBlocking {
                (1..5).asFlow().cancellable().collect { value ->
                    if (value == 3) cancel()
                    log(value)
                }
            }
        }.onFailure {
            println(it)
        }
    }
}