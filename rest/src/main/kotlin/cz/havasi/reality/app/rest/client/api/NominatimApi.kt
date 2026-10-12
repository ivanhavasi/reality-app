package cz.havasi.reality.app.rest.client.api

import cz.havasi.reality.app.rest.client.model.NominatimPlace
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

// Nominatim's usage policy requires a User-Agent that identifies the application
internal const val NOMINATIM_USER_AGENT: String = "HavasiReality/1.0 (+https://reality.havasi.me)"

@Path("/search")
@RegisterRestClient(configKey = "nominatim-api")
@ClientHeaderParam(name = "User-Agent", value = [NOMINATIM_USER_AGENT])
internal interface NominatimApi {
    // structured search: matches the street itself, a free-form query also returns shops and hospitals on it
    @GET
    suspend fun searchStreet(
        @QueryParam("street") street: String,
        @QueryParam("city") city: String,
        @QueryParam("country") country: String,
        @QueryParam("layer") layer: String,
        @QueryParam("limit") limit: Int,
        @QueryParam("format") format: String,
        @QueryParam("accept-language") acceptLanguage: String,
    ): List<NominatimPlace>
}
