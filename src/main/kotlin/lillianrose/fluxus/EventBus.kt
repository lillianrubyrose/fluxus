package lillianrose.fluxus

import kotlinx.coroutines.*
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

/**
 * Custom exception class for EventBus-related errors.
 *
 * @property message The error message
 * @property event The event that caused the exception
 * @property listener The listener that failed to process the event
 * @property cause The underlying cause of the exception
 */
@Suppress("MemberVisibilityCanBePrivate")
class EventBusException(message: String, val event: Any, val listener: Any, cause: Throwable) :
    RuntimeException(message, cause)

/**
 * Interface for handling exceptions that occur during event processing.
 */
interface EventBusExceptionHandler {
    /**
     * Handles exceptions thrown during event processing.
     *
     * @param exception The EventBusException to handle
     */
    fun handleException(exception: EventBusException)
}

/**
 * A thread-safe event bus implementation supporting both synchronous and asynchronous event handling.
 */
object EventBus {
    private val syncSubscriptions = ConcurrentHashMap<Class<*>, MutableSet<SyncEventListener<*>>>()
    private val asyncSubscriptions = ConcurrentHashMap<Class<*>, MutableSet<AsyncEventListener<*>>>()

    private val eventScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
            println("Uncaught coroutine exception: $throwable")
        })

    /**
     * Exception handler for managing errors during event processing.
     * If null, exceptions will be thrown directly.
     */
    var exceptionHandler: EventBusExceptionHandler? = null

    /**
     * Subscribes all event listeners from a given parent object to the event bus.
     *
     * @param parent The object containing event listeners to subscribe
     */
    fun subscribe(parent: Any) {
        EventListenerHolder.getSync(parent)?.forEach {
            syncSubscriptions.getOrPut(it.event) { ConcurrentSkipListSet(Collections.reverseOrder()) }.add(it)
        }

        EventListenerHolder.getAsync(parent)?.forEach {
            asyncSubscriptions.getOrPut(it.event) { ConcurrentHashMap.newKeySet() }.add(it)
        }
    }

    /**
     * Unsubscribes all event listeners associated with the given parent object.
     *
     * @param parent The object containing event listeners to unsubscribe
     */
    fun unsubscribe(parent: Any) {
        EventListenerHolder.getSync(parent)?.forEach {
            syncSubscriptions[it.event]?.remove(it)
        }

        EventListenerHolder.getAsync(parent)?.forEach {
            asyncSubscriptions[it.event]?.remove(it)
        }
    }

    /**
     * Removes all event subscriptions from the event bus.
     */
    fun unsubscribeAll() {
        syncSubscriptions.clear()
        asyncSubscriptions.clear()
    }

    /**
     * Posts an event to all relevant subscribers, both synchronous and asynchronous.
     *
     * @param event The event to post
     */
    fun post(event: Any) {
        postSync(event)
        postAsync(event)
    }

    private fun postSync(event: Any) {
        syncSubscriptions[event.javaClass]?.forEach { listener ->
            try {
                @Suppress("UNCHECKED_CAST")
                (listener as SyncEventListener<Any>).function.invoke(event)
            } catch (e: Exception) {
                val eventBusException = EventBusException(
                    "Exception during sync event handling",
                    event,
                    listener,
                    e
                )
                exceptionHandler?.handleException(eventBusException) ?: throw eventBusException
            }
        }
    }

    private fun <T : Any> postAsync(event: T) {
        val subs = asyncSubscriptions[event.javaClass] ?: return
        if (subs.isNotEmpty()) {
            subs.forEach { listener ->
                eventScope.launch {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        (listener as AsyncEventListener<Any>).function.invoke(event)
                    } catch (e: Exception) {
                        val eventBusException = EventBusException(
                            "Exception during async event handling",
                            event,
                            listener,
                            e
                        )
                        exceptionHandler?.handleException(eventBusException)
                            ?: throw eventBusException
                    }
                }
            }
        }
    }
}