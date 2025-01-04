package cz.havasi.rest.controller

import cz.havasi.service.RealityService
import io.quarkus.logging.Log
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/hello")
internal class RealityController(
    private val realityService: RealityService,
) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun searchEstates(): String {
        Log.info("Searching estates")
        realityService.fetchAndSaveApartmentsForSale()

        return "Hello world"
    }
}
