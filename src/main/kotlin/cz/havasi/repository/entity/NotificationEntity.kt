package cz.havasi.repository.entity

import cz.havasi.model.NotificationFilter
import io.quarkus.runtime.annotations.RegisterForReflection
import org.bson.codecs.pojo.annotations.BsonCreator
import org.bson.codecs.pojo.annotations.BsonDiscriminator
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.codecs.pojo.annotations.BsonProperty
import org.bson.types.ObjectId
import java.time.OffsetDateTime

@BsonDiscriminator
@RegisterForReflection
public sealed interface NotificationEntity {
    public val name: String
    public val filter: NotificationFilter
    public val updatedAt: OffsetDateTime
    public val createdAt: OffsetDateTime
    public val userId: ObjectId
    public val enabled: Boolean
}

// These classes have to have @BsonCreator annotation (and therefore @BsonProperty) because of the way how MongoDB
// deserialization of a polymorphic object works. Standard data classes don't need this annotation, however
// @BsonDiscriminator annotation is required for polymorphic deserialization, and it can't be used with data classes.
@BsonDiscriminator(value = "email")
@RegisterForReflection
public class EmailNotificationEntity @BsonCreator constructor(
    @BsonId @BsonProperty("_id") public val id: ObjectId,
    @BsonProperty("name") public override val name: String,
    @BsonProperty("filter") public override val filter: NotificationFilter,
    @BsonProperty("updatedAt") public override val updatedAt: OffsetDateTime,
    @BsonProperty("createdAt") public override val createdAt: OffsetDateTime,
    @BsonProperty("userId") public override val userId: ObjectId,
    @BsonProperty("email") public val email: String,
    @BsonProperty("enabled") public override val enabled: Boolean,
) : NotificationEntity

@BsonDiscriminator(value = "api")
@RegisterForReflection
public class WebhookNotificationEntity @BsonCreator constructor(
    @BsonId @BsonProperty("_id") public val id: ObjectId,
    @BsonProperty("name") public override val name: String,
    @BsonProperty("filter") public override val filter: NotificationFilter,
    @BsonProperty("updatedAt") public override val updatedAt: OffsetDateTime,
    @BsonProperty("createdAt") public override val createdAt: OffsetDateTime,
    @BsonProperty("userId") public override val userId: ObjectId,
    @BsonProperty("url") public val url: String,
    @BsonProperty("enabled") public override val enabled: Boolean,
) : NotificationEntity

@BsonDiscriminator(value = "discord")
@RegisterForReflection
public class DiscordWebhookNotificationEntity @BsonCreator constructor(
    @BsonId @BsonProperty("_id") public val id: ObjectId,
    @BsonProperty("name") public override val name: String,
    @BsonProperty("filter") public override val filter: NotificationFilter,
    @BsonProperty("updatedAt") public override val updatedAt: OffsetDateTime,
    @BsonProperty("createdAt") public override val createdAt: OffsetDateTime,
    @BsonProperty("userId") public override val userId: ObjectId,
    @BsonProperty("webhookId") public val webhookId: String,
    @BsonProperty("token") public val token: String,
    @BsonProperty("enabled") public override val enabled: Boolean,
) : NotificationEntity

