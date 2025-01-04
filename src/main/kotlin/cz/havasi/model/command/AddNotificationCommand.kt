package cz.havasi.model.command

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import cz.havasi.model.NotificationFilter

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = EmailNotificationCommand::class, name = "email"),
    JsonSubTypes.Type(value = WebhookNotificationCommand::class, name = "api"),
)
public sealed interface AddNotificationCommand {
    public val name: String
    public val filter: NotificationFilter
}

public data class EmailNotificationCommand(
    override val name: String,
    override val filter: NotificationFilter,
    val email: String,
) : AddNotificationCommand

public data class WebhookNotificationCommand(
    override val name: String,
    override val filter: NotificationFilter,
    val url: String,
) : AddNotificationCommand
