package cz.havasi.repository.entity

import cz.havasi.model.NotificationFilter
import org.bson.types.ObjectId
import java.time.OffsetDateTime

public data class UserEntity(
    val _id: ObjectId,
    val email: String,
    val username: String,
    val notifications: List<NotificationEntity>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

public sealed class NotificationEntity(
    public open val _id: ObjectId,
    public open val name: String,
    public open val filter: NotificationFilter,
    public open val updatedAt: OffsetDateTime,
    public open val createdAt: OffsetDateTime,
) {
    public data class EmailNotificationEntity(
        public override val _id: ObjectId,
        public override val name: String,
        public override val filter: NotificationFilter,
        public override val updatedAt: OffsetDateTime,
        public override val createdAt: OffsetDateTime,
        public val email: String,
    ) : NotificationEntity(_id, name, filter, updatedAt, createdAt)

    public data class WebhookNotificationEntity(
        public override val _id: ObjectId,
        public override val name: String,
        public override val filter: NotificationFilter,
        public override val updatedAt: OffsetDateTime,
        public override val createdAt: OffsetDateTime,
        public val url: String,
    ) : NotificationEntity(_id, name, filter, updatedAt, createdAt)
}
