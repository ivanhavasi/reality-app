package cz.havasi.controller

import cz.havasi.client.SrealityClient
import cz.havasi.client.model.SrealitySearchResult
import io.quarkus.logging.Log
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.inject.RestClient

@Path("/hello")
internal class RealityController(
    @RestClient private val srealityClient: SrealityClient,
) {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun searchEstates(): SrealitySearchResult {
        Log.info("Searching estates")
        val data = srealityClient.searchEstates(
            categoryType = 1,
            categoryMain = 1,
            localityCountryId = 112,
            localityRegionId = 10,
            limit = 22,
            offset = 0,
            lang = "cs",
            sort = "-date",
            topTimestampTo = System.currentTimeMillis(),
        )
        Log.info("Estates found size: ${data.results.size}")

        return data
    }

    @GET
    @Path("/2")
    fun test(): String {
        println("INSIDE2")
        return "Hello world"
    }

}
