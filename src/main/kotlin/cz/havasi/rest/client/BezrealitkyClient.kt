package cz.havasi.rest.client

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@Path("/vyhledat")
@RegisterRestClient(configKey = "bezrealitky-api")
internal interface BezrealitkyClient {
    @GET
    suspend fun searchEstates(
        @QueryParam("offerType") offerType: String,
        @QueryParam("estateType") estateType: String,
        @QueryParam("osm_value") osmValue: String,
        @QueryParam("regionOsmIds") regionOsmIds: String,
        @QueryParam("currency") currency: String,
        @QueryParam("location") location: String,
        @QueryParam("page") page: Int,
    ): String
}
