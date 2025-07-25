package cz.havasi.reality.app.model

import cz.havasi.reality.app.model.type.NotificationType
import io.quarkus.runtime.annotations.RegisterForReflection
import java.time.OffsetDateTime

@RegisterForReflection
public sealed interface Notification {
    public val id: String
    public val name: String
    public val userId: String
    public val filter: NotificationFilter
    public val updatedAt: OffsetDateTime
    public val createdAt: OffsetDateTime
    public val enabled: Boolean
    public val type: NotificationType
}

@RegisterForReflection
public data class EmailNotification(
    override val id: String,
    override val name: String,
    override val filter: NotificationFilter,
    override val userId: String,
    override val updatedAt: OffsetDateTime,
    override val createdAt: OffsetDateTime,
    val email: String,
    override val enabled: Boolean,
) : Notification {
    override val type: NotificationType = NotificationType.EMAIL
}

@RegisterForReflection
public data class WebhookNotification(
    override val id: String,
    override val name: String,
    override val userId: String,
    override val filter: NotificationFilter,
    override val updatedAt: OffsetDateTime,
    override val createdAt: OffsetDateTime,
    val url: String,
    override val enabled: Boolean,
) : Notification {
    override val type: NotificationType = NotificationType.WEBHOOK
}

@RegisterForReflection
public data class DiscordWebhookNotification(
    override val id: String,
    override val name: String,
    override val userId: String,
    override val filter: NotificationFilter,
    override val updatedAt: OffsetDateTime,
    override val createdAt: OffsetDateTime,
    val webhookId: String,
    val token: String, // todo hash with secret
    override val enabled: Boolean,
) : Notification {
    override val type: NotificationType = NotificationType.DISCORD
}
