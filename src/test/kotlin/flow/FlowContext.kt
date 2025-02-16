package flow

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class FlowContext {
    @Test
    fun wrongWayChangeContext() {
        fun simple()  = flow {
            // The WRONG way to change context for CPU-consuming code in flow builder
            kotlinx.coroutines.withContext(Dispatchers.Default) {
                for (i in 1..3) {
                    Thread.sleep(100) // pretend we are computing it in CPU-consuming way
                    emit(i) // emit next value
                }
            }
        }

        runBlocking {
            simple().collect { value -> log(value) }
        }
    }

    @Test
    fun changeContext() {
        fun simple() = flow {
            for (i in 1..3) {
                Thread.sleep(100) // pretend we are computing it in CPU-consuming way
                log("Emitting $i")
                emit(i) // emit next value
            }
        }.flowOn(Dispatchers.Default) // RIGHT way to change context for CPU-consuming code in flow builder

        runBlocking {
            simple().collect { value ->
                log("Collected $value")
            }
        }
    }
}