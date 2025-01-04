package cz.havasi.rest.client.model

import com.fasterxml.jackson.annotation.JsonProperty

public data class MailjetEmailsWrapper(
    @JsonProperty("Messages")
    public val messages: List<MailjetEmailResponse>,
)

public data class MailjetEmailResponse(
    @JsonProperty("Errors")
    public val mailjetEmailResponseErrors: List<MailjetEmailResponseError>? = null,
    @JsonProperty("Status")
    public val status: String,
    @JsonProperty("CustomID")
    public val customID: String? = null,
    @JsonProperty("To")
    public val to: List<MailjetEmailDetails>? = null,
    @JsonProperty("Cc")
    public val cc: List<String>? = emptyList(),
    @JsonProperty("Bcc")
    public val bcc: List<String>? = emptyList(),
)

public data class MailjetEmailResponseError(
    @JsonProperty("ErrorIdentifier")
    public val errorIdentifier: String,
    @JsonProperty("ErrorCode")
    public val errorCode: String,
    @JsonProperty("StatusCode")
    public val statusCode: Int,
    @JsonProperty("ErrorMessage")
    public val errorMessage: String,
    @JsonProperty("ErrorRelatedTo")
    public val errorRelatedTo: List<String>,
)

public data class MailjetEmailDetails(
    @JsonProperty("Email")
    public val email: String,
    @JsonProperty("MessageUUID")
    public val messageUUID: String,
    @JsonProperty("MessageID")
    public val messageID: Long,
    @JsonProperty("MessageHref")
    public val messageHref: String,
)

