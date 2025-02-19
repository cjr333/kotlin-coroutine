package flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.DEFAULT_CONCURRENCY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.internal.ChannelFlow
import kotlinx.coroutines.flow.internal.SendingCollector
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlatteningFlows {
    fun requestFlow(i: Int): Flow<String> = flow {
        emit("$i: First")
        delay(3000L - 500 * i) // wait 500 ms
        emit("$i: Second")
    }

    @Test
    fun flatMapConcat() {
        runBlocking {
            val startTime = System.currentTimeMillis() // remember the start time
            (1..3).asFlow().onEach { delay(100) } // emit a number every 100 ms
                .flatMapConcat { requestFlow(it) }
                .collect { value -> // collect and print
                    log("$value at ${System.currentTimeMillis() - startTime} ms from start")
                }
        }
    }

    @Test
    fun flatMapMerge() {
        runBlocking {
            val startTime = System.currentTimeMillis() // remember the start time
            (1..3).asFlow().onEach { delay(100) } // emit a number every 100 ms
                .flatMapMerge { requestFlow(it) }
                .collect { value -> // collect and print
                    log("$value at ${System.currentTimeMillis() - startTime} ms from start")
                }
        }
    }

    @Test
    fun flatMapLatest() {
        runBlocking {
            val startTime = System.currentTimeMillis() // remember the start time
            (1..3).asFlow().onEach { delay(100) } // emit a number every 100 ms
                .flatMapLatest { requestFlow(it) }
                .collect { value -> // collect and print
                    log("$value at ${System.currentTimeMillis() - startTime} ms from start")
                }
        }
    }

    @Test
    fun `not using flatMap`() {
        // like flatMapSequential of WebFlux
        runBlocking {
            val startTime = System.currentTimeMillis() // remember the start time
            (1..3).map {
                async { requestFlow(it).toList() }
            }.map { it.await() }.flatten().asFlow().collect { value -> // collect and print
                log("$value at ${System.currentTimeMillis() - startTime} ms from start")
            }
        }
    }

    @Test
    fun flatMapSequential() {
        // like flatMapSequential of WebFlux
        runBlocking {
            val startTime = System.currentTimeMillis() // remember the start time
            (1..3).asFlow().onEach { delay(100) } // emit a number every 100 ms
                .flatMapSequential { requestFlow(it) }
                .collect { value -> // collect and print
                    log("$value at ${System.currentTimeMillis() - startTime} ms from start")
                }
        }
    }

    @OptIn(InternalCoroutinesApi::class)
    private class ChannelFlowSequential<T>(
        private val flow: Flow<Flow<T>>,
        private val concurrency: Int,
        context: CoroutineContext = EmptyCoroutineContext,
        capacity: Int = Channel.BUFFERED,
        onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND
    ) : ChannelFlow<T>(context, capacity, onBufferOverflow) {
        val collectToFun: suspend (ProducerScope<T>) -> Unit
            get() = { collectTo(it) }

        override fun create(
            context: CoroutineContext,
            capacity: Int,
            onBufferOverflow: BufferOverflow
        ): ChannelFlow<T> =
            ChannelFlowSequential(flow, concurrency, context, capacity, onBufferOverflow)

        override fun produceImpl(scope: CoroutineScope): ReceiveChannel<T> {
            return scope.produce(context, capacity, block = collectToFun)
        }

        override suspend fun collectTo(scope: ProducerScope<T>) {
            val semaphore = Semaphore(concurrency)
            val collector = SendingCollector(scope)
            val job: Job? = coroutineContext[Job]
            var prevCollector: SequentialSendingCollector<T>? = null
            flow
                .onCompletion {
                    prevCollector?.complete()
                }.collect { inner ->
                    /*
                     * We launch a coroutine on each emitted element and the only potential
                     * suspension point in this collector is `semaphore.acquire` that rarely suspends,
                     * so we manually check for cancellation to propagate it to the upstream in time.
                     */
                    job?.ensureActive()
                    semaphore.acquire()
                    val currentCollector = SequentialSendingCollector(prevCollector, collector)
                    prevCollector?.addNext()
                    prevCollector = currentCollector
                    scope.launch {
                        try {
                            inner
                                .onCompletion {
                                    currentCollector.waitPrevCompleted()
                                    currentCollector.complete()
                                }
                                .collect(currentCollector)
                        } finally {
                            semaphore.release() // Release concurrency permit
                        }
                    }
                }
        }

        override fun additionalToStringProps(): String = "concurrency=$concurrency"

        class SequentialSendingCollector<T>(
            private var prevCollector: SequentialSendingCollector<T>? = null,
            private val sendingCollector: SendingCollector<T>
        ) : FlowCollector<T> {
            private val buffer = mutableListOf<T>()
            private var completed = false
            private var completeChannel: Channel<Unit>? = null

            override suspend fun emit(value: T) {
                if (prevCollector?.completed != false) {
                    buffer.forEach { sendingCollector.emit(it) }
                    buffer.clear()
                    sendingCollector.emit(value)
                } else {
                    buffer.add(value)
                }
            }

            fun addNext() {
                completeChannel = Channel(RENDEZVOUS)
            }

            suspend fun complete() {
                completed = true
                if (completeChannel != null) {
                    completeChannel!!.send(Unit)
                    completeChannel!!.close()
                }
            }

            suspend fun waitPrevCompleted() {
                if (prevCollector?.completed == false) {
                    prevCollector?.completeChannel?.receive()
                }
                if (buffer.isNotEmpty()) {
                    buffer.forEach { sendingCollector.emit(it) }
                    buffer.clear()
                }
            }
        }
    }

    private fun <T, R> Flow<T>.flatMapSequential(
        concurrency: Int = DEFAULT_CONCURRENCY,
        transform: suspend (value: T) -> Flow<R>
    ): Flow<R> = ChannelFlowSequential(map(transform), concurrency)
}