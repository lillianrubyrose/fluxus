package lillianrose.fluxus

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Manages the registration and storage of event listeners.
 */
object EventListenerHolder {
    private val syncListeners = ConcurrentHashMap<Any, CopyOnWriteArrayList<SyncEventListener<*>>>()
    private val asyncListeners = ConcurrentHashMap<Any, CopyOnWriteArrayList<AsyncEventListener<*>>>()

    /**
     * Adds a synchronous event listener for the given parent object.
     *
     * @param parent The object owning the listener
     * @param listener The synchronous event listener to add
     */
    fun add(parent: Any, listener: SyncEventListener<*>) {
        syncListeners.getOrPut(parent) { CopyOnWriteArrayList() }.add(listener)
    }

    /**
     * Adds an asynchronous event listener for the given parent object.
     *
     * @param parent The object owning the listener
     * @param listener The asynchronous event listener to add
     */
    fun add(parent: Any, listener: AsyncEventListener<*>) {
        asyncListeners.getOrPut(parent) { CopyOnWriteArrayList() }.add(listener)
    }

    fun remove(parent: Any) {
        syncListeners.remove(parent)
        asyncListeners.remove(parent)
    }

    fun getSync(parent: Any): List<SyncEventListener<*>>? = syncListeners[parent]
    fun getAsync(parent: Any): List<AsyncEventListener<*>>? = asyncListeners[parent]
}