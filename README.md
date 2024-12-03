# Fluxus

Fluxus is a lightweight, thread-safe event bus implementation for Kotlin/JVM that supports sync and async
event handling.

## Usage

### Basic Usage

```kotlin
// Define your event
data class UserLoggedInEvent(val username: String)

// Create a subscriber
class UserTracker {
    init {
        // Synchronous subscription
        subscribe<UserLoggedInEvent> { event ->
            println("User logged in: ${event.username}")
        }

        // Asynchronous subscription
        subscribeAsync<UserLoggedInEvent> { event ->
            // Perform async operations
            delay(100)
            println("Async handling of login: ${event.username}")
        }
    }
}

// Register the subscriber
val tracker = UserTracker()
EventBus.subscribe(tracker)

// Post an event
EventBus.post(UserLoggedInEvent("john_doe"))
```

### Priority-based Subscribers

```kotlin
class PrioritySubscriber {
    init {
        subscribe<MyEvent>(priority = EventListener.PRIORITY_HIGHEST) {
            println("This handles first!")
        }

        subscribe<MyEvent>(priority = EventListener.PRIORITY_LOWEST) {
            println("This handles last!")
        }
    }
}
```

### Exception Handling

```kotlin
EventBus.exceptionHandler = object : EventBusExceptionHandler {
    override fun handleException(exception: EventBusException) {
        println("Event handling failed: ${exception.message}")
        // Handle or log the error
    }
}
```

### Unsubscribing

```kotlin
// Unsubscribe a single subscriber
EventBus.unsubscribe(subscriber)

// Unsubscribe all
EventBus.unsubscribeAll()
```