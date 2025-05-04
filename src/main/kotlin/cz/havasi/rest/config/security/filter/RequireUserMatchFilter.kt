package cz.havasi.rest.config.security.filter

import cz.havasi.rest.config.security.RequireUserMatch
import io.quarkus.security.identity.SecurityIdentity
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ResourceInfo
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.Response.Status.FORBIDDEN
import jakarta.ws.rs.core.Response.Status.UNAUTHORIZED
import jakarta.ws.rs.ext.Provider

@Provider
@Priority(Priorities.AUTHORIZATION)
@ApplicationScoped
public class RequireUserMatchFilter(
    private val identity: SecurityIdentity,
    private val resourceInfo: ResourceInfo,
) : ContainerRequestFilter {

    override fun filter(requestContext: ContainerRequestContext): Unit {
        val annotation = resourceInfo.resourceMethod.getAnnotation(RequireUserMatch::class.java)
            ?: return // skip if the annotation is not present

        if (identity.hasRole("ADMIN")) {
            return // skip if the user has admin role
        }
        val expectedUserId = identity.getAttribute<String>(annotation.value)
        if (expectedUserId == null) {
            requestContext.abortWith(UNAUTHORIZED) // user id is not present in the identity
            return
        }
        val idInRequest = requestContext.uriInfo.pathParameters[annotation.pathParam]?.firstOrNull()
        if (idInRequest == null || idInRequest != expectedUserId) {
            requestContext.abortWith(FORBIDDEN) // user id in the request does not match the user id in the identity
        }
    }

    private fun ContainerRequestContext.abortWith(status: Response.Status) =
        abortWith(Response.status(status).build())
}
