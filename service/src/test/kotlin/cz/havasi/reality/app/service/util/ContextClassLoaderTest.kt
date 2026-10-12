package cz.havasi.reality.app.service.util

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.net.URLClassLoader
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertSame

internal class ContextClassLoaderTest {
    private val appClassLoader = URLClassLoader(emptyArray(), null)

    @Test
    fun `coroutine sees the pinned class loader on every thread it resumes on`() = runBlocking {
        Executors.newFixedThreadPool(2).asCoroutineDispatcher().use { pool ->
            val seen = withContext(pool + ContextClassLoader(appClassLoader)) {
                val before = Thread.currentThread().contextClassLoader
                yield()
                before to Thread.currentThread().contextClassLoader
            }

            assertSame(appClassLoader, seen.first)
            assertSame(appClassLoader, seen.second)
        }
    }

    @Test
    fun `thread gets its own class loader back once the coroutine leaves it`() = runBlocking {
        Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { thread ->
            val original = withContext(thread) { Thread.currentThread().contextClassLoader }

            withContext(thread + ContextClassLoader(appClassLoader)) {
                assertSame(appClassLoader, Thread.currentThread().contextClassLoader)
            }

            assertSame(original, withContext(thread) { Thread.currentThread().contextClassLoader })
        }
    }
}
