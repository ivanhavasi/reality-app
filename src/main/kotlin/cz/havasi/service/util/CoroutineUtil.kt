package cz.havasi.service.util

import io.quarkus.logging.Log
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal suspend fun launchAndHandleException(message: String, f: suspend () -> Unit) = coroutineScope {
    launch {
        try {
            Log.debug(message)
            f()
        } catch (e: Exception) {
            Log.error("Error while $message", e)
            throw e
        }
    }
}
