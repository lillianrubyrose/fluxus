package lillianrose.fluxus

import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

/**
 * Base class for event listeners with priority support.
 *
 * @param T The type of event this listener handles
 * @param F The type of function (sync or async) this listener uses
 * @property parent Weak reference to the parent object
 * @property event The class of events this listener handles
 * @property priority The priority level of this listener
 * @property function The function to execute when handling events
 */
abstract class EventListener<T : Any, F>(
    parent: Any?,
    val event: Class<T>,
    private val priority: Int = PRIORITY_DEFAULT,
    val function: F
) :
    Comparable<EventListener<T, F>> {
    @Suppress("unused")
    companion object {
        private val NEXT_ID = AtomicInteger(0)

        const val PRIORITY_DEFAULT = 0
        const val PRIORITY_LOWEST = Int.MIN_VALUE
        const val PRIORITY_HIGHEST = Int.MAX_VALUE
    }

    val id = NEXT_ID.getAndIncrement()
    val parent: WeakReference<Any?> = WeakReference(parent)

    override fun compareTo(other: EventListener<T, F>): Int {
        val result = priority.compareTo(other.priority)
        return if (result != 0) {
            result
        } else {
            id.compareTo(other.id)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EventListener<*, *>

        if (id != other.id) return false
        if (event != other.event) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + event.hashCode()
        return result
    }
}

class SyncEventListener<T : Any>(
    parent: Any?,
    eventClass: Class<T>,
    priority: Int = PRIORITY_DEFAULT,
    function: (T) -> Unit
) : EventListener<T, (T) -> Unit>(parent, eventClass, priority, function)

class AsyncEventListener<T : Any>(
    parent: Any?,
    eventClass: Class<T>,
    function: suspend (T) -> Unit
) : EventListener<T, suspend (T) -> Unit>(parent, eventClass, 0, function)

/**
 * Extension function to create a synchronous event subscription.
 *
 * @param T The type of event to subscribe to
 * @param priority The priority of the subscription
 * @param function The handler function to be called when the event occurs
 */
inline fun <reified T : Any> Any.subscribe(
    priority: Int = EventListener.PRIORITY_DEFAULT,
    noinline function: (T) -> Unit
) {
    EventListenerHolder.add(this, SyncEventListener(this, T::class.java, priority, function))
}

/**
 * Extension function to create an asynchronous event subscription.
 *
 * @param T The type of event to subscribe to
 * @param function The suspend handler function to be called when the event occurs
 */
inline fun <reified T : Any> Any.subscribeAsync(
    noinline function: suspend (T) -> Unit
) {
    EventListenerHolder.add(this, AsyncEventListener(this, T::class.java, function))
}