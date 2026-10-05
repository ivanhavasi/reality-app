package cz.havasi.reality.app.sreality.api

import cz.havasi.reality.app.model.constant.BROWSER_ACCEPT_LANGUAGE
import cz.havasi.reality.app.model.constant.CHROME_USER_AGENT
import cz.havasi.reality.app.sreality.model.SrealitySearchResult
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.jboss.resteasy.reactive.RestResponse

@Path("/api/v1")
@RegisterRestClient(configKey = "sreality-api")
@ClientHeaderParam(name = "User-Agent", value = [CHROME_USER_AGENT])
@ClientHeaderParam(name = "Accept-Language", value = [BROWSER_ACCEPT_LANGUAGE])
internal interface SrealityApi {
    @GET
    @Path("/estates/search")
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun searchEstates(
        @QueryParam("category_type_cb") categoryType: Int,
        @QueryParam("category_main_cb") categoryMain: Int,
        @QueryParam("category_sub_cb") categorySub: String?,
        @QueryParam("locality_country_id") localityCountryId: Int,
        @QueryParam("locality_region_id") localityRegionId: Int,
        @QueryParam("limit") limit: Int,
        @QueryParam("offset") offset: Int,
        @QueryParam("lang") lang: String,
        @QueryParam("sort") sort: String,
        @QueryParam("top_timestamp_to") topTimestampTo: Long,
    ): RestResponse<SrealitySearchResult>
}
