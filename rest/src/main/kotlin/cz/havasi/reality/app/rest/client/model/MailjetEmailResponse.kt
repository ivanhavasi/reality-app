package cz.havasi.reality.app.rest.client.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
internal data class MailjetEmailsWrapper(
    @JsonProperty("Messages")
    internal val messages: List<MailjetEmailResponse>,
)

@RegisterForReflection
internal data class MailjetEmailResponse(
    @JsonProperty("Errors")
    internal val mailjetEmailResponseErrors: List<MailjetEmailResponseError>? = null,
    @JsonProperty("Status")
    internal val status: String,
    @JsonProperty("CustomID")
    internal val customID: String? = null,
    @JsonProperty("To")
    internal val to: List<MailjetEmailDetails>? = null,
    @JsonProperty("Cc")
    internal val cc: List<String>? = emptyList(),
    @JsonProperty("Bcc")
    internal val bcc: List<String>? = emptyList(),
)

@RegisterForReflection
internal data class MailjetEmailResponseError(
    @JsonProperty("ErrorIdentifier")
    internal val errorIdentifier: String,
    @JsonProperty("ErrorCode")
    internal val errorCode: String,
    @JsonProperty("StatusCode")
    internal val statusCode: Int,
    @JsonProperty("ErrorMessage")
    internal val errorMessage: String,
    @JsonProperty("ErrorRelatedTo")
    internal val errorRelatedTo: List<String>,
)

@RegisterForReflection
internal data class MailjetEmailDetails(
    @JsonProperty("Email")
    internal val email: String,
    @JsonProperty("MessageUUID")
    internal val messageUUID: String,
    @JsonProperty("MessageID")
    internal val messageID: Long,
    @JsonProperty("MessageHref")
    internal val messageHref: String,
)

