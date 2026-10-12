package cz.havasi.reality.app.rest.controller

import cz.havasi.reality.app.model.FavoriteListing
import cz.havasi.reality.app.rest.controller.util.wrapToNoContent
import cz.havasi.reality.app.rest.controller.util.wrapToOk
import cz.havasi.reality.app.rest.security.RequireUserMatch
import cz.havasi.reality.app.service.FavoriteService
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.jboss.resteasy.reactive.RestResponse

@Path("/api/users/{userId}/favorites")
@Produces(MediaType.APPLICATION_JSON)
internal open class FavoriteController(
    private val favoriteService: FavoriteService,
) {
    @GET
    @RequireUserMatch
    open suspend fun getFavorites(
        @PathParam("userId") userId: String,
    ): RestResponse<List<FavoriteListing>> =
        favoriteService.getFavorites(userId).wrapToOk()

    // PUT and DELETE are idempotent, so a retried or doubled click is harmless
    @PUT
    @RequireUserMatch
    @Path("/{listingId}")
    open suspend fun star(
        @PathParam("userId") userId: String,
        @PathParam("listingId") listingId: String,
    ): RestResponse<Nothing> =
        try {
            favoriteService.star(userId, listingId).wrapToNoContent()
        } catch (e: NoSuchElementException) {
            throw NotFoundException(e.message)
        }

    @DELETE
    @RequireUserMatch
    @Path("/{listingId}")
    open suspend fun unstar(
        @PathParam("userId") userId: String,
        @PathParam("listingId") listingId: String,
    ): RestResponse<Nothing> =
        favoriteService.unstar(userId, listingId).wrapToNoContent()
}
