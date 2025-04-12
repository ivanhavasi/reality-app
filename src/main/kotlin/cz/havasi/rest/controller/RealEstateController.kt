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

@Path("/real-estates")
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
        @QueryParam("offset") offset: Int = 0,
        @QueryParam("limit") limit: Int = 20,
        @QueryParam("sortDirection") sortDirection: String = "DESC",
    ): RestResponse<List<Apartment>> =
        realEstateService.getApartments(
            Paging(
                offset = offset.coerceIn(0, 20),
                limit = limit.coerceIn(0, 20),
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
