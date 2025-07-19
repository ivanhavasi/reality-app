package cz.havasi.reality.app.rest.controller

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.type.UserRole.Companion.ADMIN_ROLE
import cz.havasi.reality.app.model.type.UserRole.Companion.USER_ROLE
import cz.havasi.reality.app.model.util.Paging
import cz.havasi.reality.app.model.util.SortDirection
import cz.havasi.reality.app.rest.controller.util.wrapToNoContent
import cz.havasi.reality.app.rest.controller.util.wrapToOk
import cz.havasi.reality.app.service.RealEstateService
import jakarta.annotation.security.RolesAllowed
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import org.jboss.resteasy.reactive.RestResponse

@Path("/api/real-estates")
@Produces(MediaType.APPLICATION_JSON)
internal open class RealEstateController(
    private val realEstateService: RealEstateService,
) {
    @POST
    @RolesAllowed(ADMIN_ROLE)
    @Path("/process")
    open suspend fun processNewRealEstates(): RestResponse<Nothing> =
        realEstateService
            .fetchAndSaveRealEstate(BuildingType.APARTMENT, TransactionType.SALE)
            .wrapToNoContent()

    @GET
    @RolesAllowed(USER_ROLE)
    open suspend fun getRealEstates(
        @DefaultValue("0") @QueryParam("offset") offset: Int,
        @DefaultValue("20") @QueryParam("limit") limit: Int,
        @DefaultValue("DESC") @QueryParam("sortDirection") sortDirection: String,
        @QueryParam("search") searchString: String? = null,
    ): RestResponse<List<Apartment>> =
        realEstateService.getApartments(
            searchString,
            Paging(
                offset = offset.coerceAtLeast(0),
                limit = limit.coerceIn(10, 20),
                sortDirection = sortDirection.toSortDirection(),
            ),
        )
            .wrapToOk()

    private fun String.toSortDirection() = when (this) {
        "ASC" -> SortDirection.ASC
        "DESC" -> SortDirection.DESC
        else -> SortDirection.DESC
    }
}
