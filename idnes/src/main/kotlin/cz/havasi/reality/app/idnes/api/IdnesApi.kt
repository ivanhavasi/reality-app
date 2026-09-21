package cz.havasi.reality.app.idnes.api

import cz.havasi.reality.app.model.constant.BROWSER_ACCEPT_LANGUAGE
import cz.havasi.reality.app.model.constant.CHROME_USER_AGENT
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.jboss.resteasy.reactive.RestResponse

@Path("/s")
@RegisterRestClient(configKey = "idnes-api")
@ClientHeaderParam(name = "User-Agent", value = [CHROME_USER_AGENT])
@ClientHeaderParam(name = "Accept-Language", value = [BROWSER_ACCEPT_LANGUAGE])
internal interface IdnesApi {
    @GET
    @Path("/{transactionType}/{buildingType}/{location}/")
    @Produces(MediaType.TEXT_HTML)
    suspend fun searchEstatesForPageZero(
        @PathParam("transactionType") transactionType: String,
        @PathParam("buildingType") buildingType: String,
        @PathParam("location") location: String,
        @QueryParam("page") page: Int?,
    ): RestResponse<String>

    @GET
    @Path("/{transactionType}/{buildingType}/{location}/")
    @Produces(MediaType.TEXT_HTML)
    suspend fun searchEstatesForOtherPages(
        @PathParam("transactionType") transactionType: String,
        @PathParam("buildingType") buildingType: String,
        @PathParam("location") location: String,
        @QueryParam("page") page: Int,
    ): RestResponse<String>
}
