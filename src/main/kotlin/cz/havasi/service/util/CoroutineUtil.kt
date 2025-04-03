package cz.havasi.service.util

import io.quarkus.logging.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll

//internal suspend fun launchAndHandleException(message: String, f: suspend () -> Unit) =
//    async {
//        Log.info("STARTTTT")
//        try {
//            Log.debug(message)

internal suspend fun <T> List<T>.forEachAsync(message: String, f: suspend (T) -> Unit) =
    coroutineScope {
        Log.info(message)
        val jobs = map {
            async {
                Log.info("STARTTTT")
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
