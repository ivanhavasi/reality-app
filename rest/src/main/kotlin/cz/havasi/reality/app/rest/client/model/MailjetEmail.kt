package cz.havasi.reality.app.rest.client.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
internal data class MailjetEmailWrapper(
    @JsonProperty("Messages")
    internal val messages: List<MailjetEmail>,
    @JsonProperty("SandboxMode")
    internal val sandboxMode: Boolean = false, // if the email should be sent in sandbox mode (not really sent)
)

@RegisterForReflection
internal data class MailjetEmail(
    @JsonProperty("From")
    internal val from: EmailAddress,
    @JsonProperty("To")
    internal val to: List<EmailAddress>,
    @JsonProperty("Subject")
    internal val subject: String,
    @JsonProperty("TextPart")
    internal val textPart: String,
    @JsonProperty("HTMLPart")
    internal val htmlPart: String,
)

@RegisterForReflection
internal data class EmailAddress(
    @JsonProperty("Email")
    internal val email: String,
    @JsonProperty("Name")
    internal val name: String,
)
