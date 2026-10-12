package cz.havasi.reality.app.service.util

import io.quarkus.logging.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlin.coroutines.CoroutineContext

internal suspend fun <T> List<T>.forEachAsync(message: String, f: suspend (T) -> Unit) =
    coroutineScope {
        Log.info(message)
        val jobs = map {
            async {
                try {
                    Log.debug(message)
                    f(it)
                    Log.info("Finished $message")
                } catch (e: Exception) {
                    Log.error("Error while $message", e)
                }
            }
        }
        jobs.joinAll()
    }

internal suspend fun <T> List<T>.forEachSequentially(message: String, f: suspend (T) -> Unit) {
    Log.info(message)
    forEach {
        try {
            f(it)
            Log.info("Finished $message")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.error("Error while $message", e)
        }
    }
}

// threads Quarkus didn't create (Dispatchers.IO) carry a context class loader that can't see the app's classes
// (in dev mode it may even be the one from before a live reload), and REST clients are built lazily by class name
// through it; this pins the given loader for as long as the coroutine runs on a thread
internal class ContextClassLoader(private val classLoader: ClassLoader) : ThreadContextElement<ClassLoader?> {
    companion object Key : CoroutineContext.Key<ContextClassLoader>

    override val key: CoroutineContext.Key<ContextClassLoader> get() = Key

    override fun updateThreadContext(context: CoroutineContext): ClassLoader? {
        val thread = Thread.currentThread()
        return thread.contextClassLoader.also { thread.contextClassLoader = classLoader }
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: ClassLoader?) {
        Thread.currentThread().contextClassLoader = oldState
    }
}
