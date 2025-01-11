package cz.havasi.rest.client

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@Path("/s")
@RegisterRestClient(configKey = "idnes-api")
internal interface IdnesClient {

    @GET
    @Path("/{transactionType}/{buildingType}/{location}/")
    @Produces(MediaType.TEXT_HTML)
    suspend fun searchEstatesForPageZero(
        @PathParam("transactionType") transactionType: String,
        @PathParam("buildingType") buildingType: String,
        @PathParam("location") location: String,
        @QueryParam("page") page: Int?,
    ): String // todo RestResponse<String> and handle it somewhere in rest module

    @GET
    @Path("/{transactionType}/{buildingType}/{location}/")
    @Produces(MediaType.TEXT_HTML)
    suspend fun searchEstatesForOtherPages(
        @PathParam("transactionType") transactionType: String,
        @PathParam("buildingType") buildingType: String,
        @PathParam("location") location: String,
        @QueryParam("page") page: Int,
    ): String // todo RestResponse<String> and handle it somewhere in rest module
}
