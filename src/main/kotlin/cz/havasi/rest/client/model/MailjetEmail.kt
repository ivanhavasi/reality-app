package cz.havasi.rest.client.model

import com.fasterxml.jackson.annotation.JsonProperty

public data class MailjetEmailWrapper(
    @JsonProperty("Messages")
    public val messages: List<MailjetEmail>,
    @JsonProperty("SandboxMode")
    public val sandboxMode: Boolean = false, // if the email should be sent in sandbox mode (not really sent)
)

public data class MailjetEmail(
    @JsonProperty("From")
    public val from: EmailAddress,
    @JsonProperty("To")
    public val to: List<EmailAddress>,
    @JsonProperty("Subject")
    public val subject: String,
    @JsonProperty("TextPart")
    public val textPart: String,
    @JsonProperty("HTMLPart")
    public val htmlPart: String,
)

public data class EmailAddress(
    @JsonProperty("Email")
    public val email: String,
    @JsonProperty("Name")
    public val name: String,
)
