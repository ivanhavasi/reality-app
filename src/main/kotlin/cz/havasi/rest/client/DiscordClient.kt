package cz.havasi.rest.client

import cz.havasi.rest.client.model.DiscordWebhookBody
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.jboss.resteasy.reactive.RestResponse

@Path("/api")
@RegisterRestClient(configKey = "discord-api")
internal interface DiscordClient {
    @Path("/webhooks/{webhookId}/{webhookToken}")
    suspend fun sendWebhook(
        @PathParam("webhookId") webhookId: String,
        @PathParam("webhookToken") webhookToken: String,
        body: DiscordWebhookBody,
    ): RestResponse<String>
}
