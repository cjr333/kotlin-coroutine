package flow

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class FlowOperators {
    @Test
    fun mapTest() {
        suspend fun performRequest(request: Int): String {
            delay(1000) // imitate long-running asynchronous work
            return "response $request"
        }

        runBlocking<Unit> {
            (1..3).asFlow() // a flow of requests
                .map { request -> performRequest(request) } // extension method
                .collect { response -> log(response) }
        }
    }

    @Test
    fun transformTest() {
        suspend fun performRequest(request: Int): String {
            delay(1000) // imitate long-running asynchronous work
            return "response $request"
        }

        runBlocking<Unit> {
            (1..3).asFlow() // a flow of requests
                .transform { request ->
                    emit("Making request $request")
                    emit(performRequest(request))
                }
                .collect { response -> log(response) }
        }
    }

    @Test
    fun takeTest() {
        fun numbers() = flow {
            try {
                emit(1)
                emit(2)
                log("This line will not execute")
                emit(3)
            } catch (ex: CancellationException) {
                log("Coroutine is cancelled. $ex")
            } finally {
                log("Finally in numbers")
            }
        }

        runBlocking {
            numbers()
                .take(2) // take only the first two
                .collect { value -> log(value) }
        }
    }

    @Test
    fun terminalFlowTest() {
        runBlocking {
            val sum = (1..5).asFlow()
                .map { it * it } // squares of numbers from 1 to 5
                .reduce { a, b -> a + b } // sum them (terminal operator)
            log(sum)
        }
    }

    @Test
    fun `first vs single`() {
        runBlocking {
            println(flowOf(1, 2).first())
            println(flowOf(1, 2).single())
        }
    }
}