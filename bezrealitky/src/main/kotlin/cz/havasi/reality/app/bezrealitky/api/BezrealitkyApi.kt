package cz.havasi.reality.app.bezrealitky.api

import cz.havasi.reality.app.model.constant.BROWSER_ACCEPT_LANGUAGE
import cz.havasi.reality.app.model.constant.CHROME_USER_AGENT
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@Path("/vyhledat")
@RegisterRestClient(configKey = "bezrealitky-api")
@ClientHeaderParam(name = "User-Agent", value = [CHROME_USER_AGENT])
@ClientHeaderParam(name = "Accept-Language", value = [BROWSER_ACCEPT_LANGUAGE])
internal interface BezrealitkyApi {
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
