package cz.havasi.reality.app.rest.client.api

import cz.havasi.reality.app.rest.client.model.OverpassResponse
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.FormParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@Path("/interpreter")
@RegisterRestClient(configKey = "overpass-api")
@ClientHeaderParam(name = "User-Agent", value = [NOMINATIM_USER_AGENT])
internal interface OverpassApi {
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    suspend fun query(@FormParam("data") query: String): OverpassResponse
}
