package cz.havasi.model

import java.time.OffsetDateTime

public sealed class Notification(
    public open val id: String,
    public open val name: String,
    public open val filter: NotificationFilter,
    public open val updatedAt: OffsetDateTime,
    public open val createdAt: OffsetDateTime,
) {
    public data class EmailNotification(
        public override val id: String,
        public override val name: String,
        public override val filter: NotificationFilter,
        public override val updatedAt: OffsetDateTime,
        public override val createdAt: OffsetDateTime,
        public val email: String,
    ) : Notification(id, name, filter, updatedAt, createdAt)

    public data class WebhookNotification(
        public override val id: String,
        public override val name: String,
        public override val filter: NotificationFilter,
        public override val updatedAt: OffsetDateTime,
        public override val createdAt: OffsetDateTime,
        public val url: String,
    ) : Notification(id, name, filter, updatedAt, createdAt)
    // todo add more notification types: discord integration?, sms, push notification
}
