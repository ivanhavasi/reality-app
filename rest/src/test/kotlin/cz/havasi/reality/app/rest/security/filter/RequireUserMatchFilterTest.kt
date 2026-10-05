package cz.havasi.reality.app.rest.security.filter

import cz.havasi.reality.app.model.type.UserRole
import cz.havasi.reality.app.rest.security.RequireUserMatch
import io.quarkus.security.identity.SecurityIdentity
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ResourceInfo
import jakarta.ws.rs.core.MultivaluedHashMap
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class RequireUserMatchFilterTest {
    @Suppress("unused")
    private class Resource {
        @RequireUserMatch
        fun guarded() = Unit
    }

    private inline fun <reified T> proxy(crossinline answer: (name: String, args: Array<out Any?>) -> Any?): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
            answer(method.name, args ?: emptyArray())
        } as T

    private fun abortStatus(roles: Set<String>, identityUserId: String, pathUserId: String): Int? {
        var aborted: Response? = null
        val identity = proxy<SecurityIdentity> { name, args ->
            when (name) {
                "hasRole" -> args[0] in roles
                "getAttribute" -> if (args[0] == "id") identityUserId else null
                else -> error("unexpected $name")
            }
        }
        val resourceInfo = proxy<ResourceInfo> { name, _ ->
            if (name == "getResourceMethod") Resource::class.java.getDeclaredMethod("guarded") else error("unexpected $name")
        }
        val uriInfo = proxy<UriInfo> { name, _ ->
            if (name == "getPathParameters") MultivaluedHashMap<String, String>().apply { add("userId", pathUserId) } else error("unexpected $name")
        }
        val context = proxy<ContainerRequestContext> { name, args ->
            when (name) {
                "getUriInfo" -> uriInfo
                "abortWith" -> null.also { aborted = args[0] as Response }
                else -> error("unexpected $name")
            }
        }

        RequireUserMatchFilter(identity, resourceInfo).filter(context)
        return aborted?.status
    }

    @Test
    fun `user cannot access another user's resources`() {
        assertEquals(403, abortStatus(setOf(UserRole.USER_ROLE), identityUserId = "me", pathUserId = "someone-else"))
    }

    @Test
    fun `user can access own resources`() {
        assertNull(abortStatus(setOf(UserRole.USER_ROLE), identityUserId = "me", pathUserId = "me"))
    }

    @Test
    fun `admin can access any user's resources`() {
        assertNull(abortStatus(setOf(UserRole.USER_ROLE, UserRole.ADMIN_ROLE), identityUserId = "me", pathUserId = "someone-else"))
    }
}
