import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import lillianrose.fluxus.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventBusTest {
    @BeforeEach
    fun setup() {
        EventBus.exceptionHandler = null
        EventBus.unsubscribeAll()
    }

    data class TestEvent(val message: String)
    data class AnotherTestEvent(val number: Int)

    class SyncSubscriber {
        var receivedMessage = ""

        init {
            subscribe<TestEvent> {
                receivedMessage = it.message
            }
        }
    }

    class AsyncSubscriber {
        val counter = AtomicInteger(0)

        init {
            subscribeAsync<TestEvent> {
                delay(10)
                counter.incrementAndGet()
            }
        }
    }

    @Test
    fun `test sync event posting`() {
        val subscriber = SyncSubscriber()
        EventBus.subscribe(subscriber)

        val testMessage = "Hello, World!"
        EventBus.post(TestEvent(testMessage))

        assertEquals(testMessage, subscriber.receivedMessage)
    }

    @Test
    fun `test async event posting`() = runBlocking {
        val subscriber = AsyncSubscriber()
        EventBus.subscribe(subscriber)

        EventBus.post(TestEvent("Test"))
        delay(50)

        assertEquals(1, subscriber.counter.get())
    }

    @Test
    fun `test multiple subscribers`() {
        val subscriber1 = SyncSubscriber()
        val subscriber2 = SyncSubscriber()

        EventBus.subscribe(subscriber1)
        EventBus.subscribe(subscriber2)

        val testMessage = "Hello, World!"
        EventBus.post(TestEvent(testMessage))

        assertEquals(testMessage, subscriber1.receivedMessage)
        assertEquals(testMessage, subscriber2.receivedMessage)
    }

    @Test
    fun `test unsubscribe`() {
        val subscriber = SyncSubscriber()
        EventBus.subscribe(subscriber)
        EventBus.unsubscribe(subscriber)

        EventBus.post(TestEvent("Test"))

        assertEquals("", subscriber.receivedMessage)
    }

    @Test
    fun `test event inheritance`() {
        open class BaseEvent
        class ChildEvent : BaseEvent()

        class InheritanceSubscriber {
            var baseReceived = false

            init {
                subscribe<BaseEvent> {
                    baseReceived = true
                }
            }
        }

        val subscriber = InheritanceSubscriber()
        EventBus.subscribe(subscriber)

        EventBus.post(ChildEvent())

        assertFalse(subscriber.baseReceived)
    }

    @Test
    fun `test multiple event types`() {
        class MultiSubscriber {
            var stringMessage = ""
            var numberValue = 0

            init {
                subscribe<TestEvent> {
                    stringMessage = it.message
                }
                subscribe<AnotherTestEvent> {
                    numberValue = it.number
                }
            }
        }

        val subscriber = MultiSubscriber()
        EventBus.subscribe(subscriber)

        EventBus.post(TestEvent("Test"))
        EventBus.post(AnotherTestEvent(42))

        assertEquals("Test", subscriber.stringMessage)
        assertEquals(42, subscriber.numberValue)
    }

    @Test
    fun `test concurrent async events`() = runBlocking {
        class ConcurrentSubscriber {
            val counter = AtomicInteger(0)

            init {
                subscribeAsync<TestEvent> {
                    delay(10)
                    counter.incrementAndGet()
                }
            }
        }

        val subscriber = ConcurrentSubscriber()
        EventBus.subscribe(subscriber)

        repeat(5) {
            EventBus.post(TestEvent("Test $it"))
        }

        delay(100)
        assertEquals(5, subscriber.counter.get())
    }

    @Test
    fun `test error handling in event handlers`() {
        class ErrorSubscriber {
            init {
                subscribe<TestEvent> {
                    throw RuntimeException("Test exception")
                }
            }
        }

        val subscriber = ErrorSubscriber()
        EventBus.subscribe(subscriber)

        assertThrows<RuntimeException> {
            EventBus.post(TestEvent("Test"))
        }
    }

    @Test
    fun `test exception handling`() = runBlocking {
        val exceptionsCaught = AtomicInteger()

        EventBus.exceptionHandler = object : EventBusExceptionHandler {
            override fun handleException(exception: EventBusException) {
                exceptionsCaught.incrementAndGet()
            }
        }

        class ErrorSubscriber {
            init {
                subscribe<TestEvent> {
                    throw RuntimeException("Sync error")
                }

                subscribeAsync<TestEvent> {
                    throw RuntimeException("Async error")
                }
            }
        }

        val subscriber = ErrorSubscriber()
        EventBus.subscribe(subscriber)

        EventBus.post(TestEvent("test"))
        delay(10)

        assertTrue(exceptionsCaught.get() == 2)
    }
}