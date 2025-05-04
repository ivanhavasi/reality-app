package cz.havasi.reality.app.idnes.api

import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.jboss.resteasy.reactive.RestResponse

@Path("/s")
@RegisterRestClient(configKey = "idnes-api")
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
