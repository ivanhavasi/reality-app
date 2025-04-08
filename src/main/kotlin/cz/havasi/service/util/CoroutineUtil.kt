package cz.havasi.service.util

import io.quarkus.logging.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll

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
