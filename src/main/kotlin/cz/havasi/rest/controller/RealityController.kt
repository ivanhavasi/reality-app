package cz.havasi.rest.controller

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/hello") // todo for testing purposes only
internal class RealityController(
//    private val idnesProvider: IdnesProvider,
) {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun searchEstates(): String {
//        idnesProvider.getEstates(
//            GetEstatesCommand(
//                transaction = TransactionType.SALE,
//                type = BuildingType.APARTMENT,
//                offset = 22,
//                limit = 22,
//            ),
//        )
        return "Hello world"
    }
}
