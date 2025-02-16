package flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.test.Test

class LaunchingFlow {
    @Test
    fun `wait until flow finished`() {
        fun events() = (1..3).asFlow().onEach { delay(100) }
        runBlocking {
            events()
                .onEach { event -> log("Event: $event") }
                .collect {} // <--- Collecting the flow waits
            log("Done")
        }
    }

    @Test
    fun launchIn() {
        fun events() = (1..3).asFlow().onEach { delay(100) }
        runBlocking {
            events()
                .onEach { event -> log("Event: $event") }
                .launchIn(this)
            log("Done")
        }
    }

    @Test
    fun `launchIn and flowOn`() {
        fun events() = (1..3).asFlow().onEach { delay(100) }
        runBlocking {
            coroutineScope {
                events()
                    .onEach { event -> log("Event1: $event") }
                    .flowOn(Dispatchers.IO)
                    .onEach { event -> log("Event2: $event") }
                    .flowOn(Dispatchers.Default)
                    .launchIn(this)
                log("Inner Done")
            }
            log("Done")
        }
    }
}