package cz.havasi.reality.app.rest.client.api

import cz.havasi.reality.app.rest.client.model.DiscordWebhookBody
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.jboss.resteasy.reactive.RestResponse

@Path("/api")
@RegisterRestClient(configKey = "discord-api")
internal interface DiscordApi {
    @POST
    @Path("/webhooks/{webhookId}/{webhookToken}")
    suspend fun sendWebhook(
        @PathParam("webhookId") webhookId: String,
        @PathParam("webhookToken") webhookToken: String,
        body: DiscordWebhookBody,
    ): RestResponse<String>
}
