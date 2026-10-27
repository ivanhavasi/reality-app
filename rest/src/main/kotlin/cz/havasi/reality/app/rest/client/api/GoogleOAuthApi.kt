package cz.havasi.reality.app.rest.client.api

import cz.havasi.reality.app.rest.client.model.GoogleTokenResponse
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.FormParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@Path("/token")
@RegisterRestClient(configKey = "google-oauth-api")
internal interface GoogleOAuthApi {
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    suspend fun exchangeCode(
        @FormParam("code") code: String,
        @FormParam("client_id") clientId: String,
        @FormParam("client_secret") clientSecret: String,
        @FormParam("redirect_uri") redirectUri: String,
        @FormParam("grant_type") grantType: String,
    ): GoogleTokenResponse

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    suspend fun refresh(
        @FormParam("refresh_token") refreshToken: String,
        @FormParam("client_id") clientId: String,
        @FormParam("client_secret") clientSecret: String,
        @FormParam("grant_type") grantType: String,
    ): GoogleTokenResponse
}
