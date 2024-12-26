package cz.havasi.client

import cz.havasi.client.model.SrealitySearchResult
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(configKey = "sreality-api")
@Path("/api/v1/estates/search")
internal interface SrealityClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun searchEstates(
        @QueryParam("category_type_cb") categoryType: Int,
        @QueryParam("category_main_cb") categoryMain: Int,
        @QueryParam("locality_country_id") localityCountryId: Int,
        @QueryParam("locality_region_id") localityRegionId: Int,
        @QueryParam("limit") limit: Int,
        @QueryParam("offset") offset: Int,
        @QueryParam("lang") lang: String,
        @QueryParam("sort") sort: String,
        @QueryParam("top_timestamp_to") topTimestampTo: Long
    ): SrealitySearchResult
}
