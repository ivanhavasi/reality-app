package cz.havasi.rest.controller

import cz.havasi.service.RealityService
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/hello") // todo for testing purposes only
internal class RealityController(
//    private val idnesProvider: IdnesProvider,
//    private val bezrealitkyProvider: BezrealitkyProvider,
    private val realityService: RealityService,
) {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun searchEstates(): String {
//        bezrealitkyProvider.getEstates(
//            GetEstatesCommand(
//                transaction = TransactionType.SALE,
//                type = BuildingType.APARTMENT,
//                offset = 0,
//                limit = 22,
//            ),
//        )
        realityService.fetchAndSaveApartmentsForSale()
        return "Hello world"
    }
}
