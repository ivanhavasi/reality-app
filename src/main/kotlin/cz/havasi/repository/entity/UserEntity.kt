package cz.havasi.repository.entity

import cz.havasi.model.NotificationFilter
import org.bson.codecs.pojo.annotations.BsonCreator
import org.bson.codecs.pojo.annotations.BsonDiscriminator
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.codecs.pojo.annotations.BsonProperty
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

@BsonDiscriminator
public sealed interface NotificationEntity {
    public val name: String
    public val filter: NotificationFilter
    public val updatedAt: OffsetDateTime
    public val createdAt: OffsetDateTime
}

// These classes have to have @BsonCreator annotation (and therefore @BsonProperty) because of the way how MongoDB
// deserialization of a polymorphic object works. Standard data classes don't need this annotation, however
// @BsonDiscriminator annotation is required for polymorphic deserialization, and it can't be used with data classes.
@BsonDiscriminator(value = "email")
public class EmailNotificationEntity @BsonCreator constructor(
    @BsonId @BsonProperty("_id") public val id: ObjectId,
    @BsonProperty("name") public override val name: String,
    @BsonProperty("filter") public override val filter: NotificationFilter,
    @BsonProperty("updatedAt") public override val updatedAt: OffsetDateTime,
    @BsonProperty("createdAt") public override val createdAt: OffsetDateTime,
    @BsonProperty("email") public val email: String,
) : NotificationEntity

@BsonDiscriminator(value = "api")
public class WebhookNotificationEntity @BsonCreator constructor(
    @BsonId @BsonProperty("_id") public val id: ObjectId,
    @BsonProperty("name") public override val name: String,
    @BsonProperty("filter") public override val filter: NotificationFilter,
    @BsonProperty("updatedAt") public override val updatedAt: OffsetDateTime,
    @BsonProperty("createdAt") public override val createdAt: OffsetDateTime,
    @BsonProperty("url") public val url: String,
) : NotificationEntity
