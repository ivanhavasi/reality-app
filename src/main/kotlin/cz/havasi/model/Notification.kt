package cz.havasi.model

import java.time.OffsetDateTime

public sealed interface Notification {
    public val id: String
    public val name: String
    public val filter: NotificationFilter
    public val updatedAt: OffsetDateTime
    public val createdAt: OffsetDateTime
}

public data class EmailNotification(
    override val id: String,
    override val name: String,
    override val filter: NotificationFilter,
    override val updatedAt: OffsetDateTime,
    override val createdAt: OffsetDateTime,
    val email: String,
) : Notification

public data class WebhookNotification(
    override val id: String,
    override val name: String,
    override val filter: NotificationFilter,
    override val updatedAt: OffsetDateTime,
    override val createdAt: OffsetDateTime,
    val url: String,
) : Notification

// todo add more notification types: discord integration?, sms, push notification
