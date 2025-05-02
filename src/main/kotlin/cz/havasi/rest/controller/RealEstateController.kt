package cz.havasi.rest.controller

import cz.havasi.model.Apartment
import cz.havasi.model.util.Paging
import cz.havasi.model.util.SortDirection
import cz.havasi.rest.controller.util.wrapToNoContent
import cz.havasi.rest.controller.util.wrapToOk
import cz.havasi.service.RealEstateService
import jakarta.annotation.security.RolesAllowed
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jboss.resteasy.reactive.RestResponse

@Path("/api/real-estates")
@Produces(MediaType.APPLICATION_JSON)
internal open class RealEstateController(
    private val realEstateService: RealEstateService,
) {
    @POST
    @RolesAllowed("ADMIN")
    @Path("/process")
    fun processNewRealEstates(): RestResponse<Nothing> =
        CoroutineScope(Dispatchers.IO).launch {
            realEstateService.fetchAndSaveApartmentsForSale()
        }
            .wrapToNoContent()

    @GET
    @RolesAllowed("USER")
    open suspend fun getRealEstates(
        @DefaultValue("0") @QueryParam("offset") offset: Int,
        @DefaultValue("20") @QueryParam("limit") limit: Int,
        @DefaultValue("DESC") @QueryParam("sortDirection") sortDirection: String,
    ): RestResponse<List<Apartment>> =
        realEstateService.getApartments(
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
