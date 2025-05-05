package cz.havasi.reality.app.model.command

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import cz.havasi.reality.app.model.NotificationFilter

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = EmailNotificationCommand::class, name = "email"),
    JsonSubTypes.Type(value = WebhookNotificationCommand::class, name = "api"),
    JsonSubTypes.Type(value = DiscordWebhookNotificationCommand::class, name = "discord"),
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

public data class DiscordWebhookNotificationCommand(
    override val name: String,
    override val filter: NotificationFilter,
    val webhookId: String,
    val token: String,
) : AddNotificationCommand
