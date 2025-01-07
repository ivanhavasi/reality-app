package cz.havasi.rest.client

import cz.havasi.rest.client.model.MailjetEmailWrapper
import cz.havasi.rest.client.model.MailjetEmailsWrapper
import io.quarkus.rest.client.reactive.ClientBasicAuth
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.jboss.resteasy.reactive.RestResponse

@Path("/v3.1")
@RegisterRestClient(configKey = "mailjet-api")
@ClientBasicAuth(
    username = "\${reality.mailjet.auth.username}",
    password = "\${reality.mailjet.auth.password}",
)
internal interface MailjetClient {
    @Path("/send")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun sendEmails(body: MailjetEmailWrapper): RestResponse<MailjetEmailsWrapper>
}
