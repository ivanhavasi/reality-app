package cz.havasi.reality.app.mongo.entity

import cz.havasi.reality.app.model.NotificationFilter
import io.quarkus.runtime.annotations.RegisterForReflection
import org.bson.codecs.pojo.annotations.BsonCreator
import org.bson.codecs.pojo.annotations.BsonDiscriminator
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.codecs.pojo.annotations.BsonProperty
import org.bson.types.ObjectId
import java.time.OffsetDateTime

@BsonDiscriminator
@RegisterForReflection
internal sealed interface NotificationEntity {
    val id: ObjectId
    val name: String
    val filter: NotificationFilter
    val updatedAt: OffsetDateTime
    val createdAt: OffsetDateTime
    val userId: ObjectId
    val enabled: Boolean
}

// These classes have to have @BsonCreator annotation (and therefore @BsonProperty) because of the way how MongoDB
// deserialization of a polymorphic object works. Standard data classes don't need this annotation, however
// @BsonDiscriminator annotation is required for polymorphic deserialization, and it can't be used with data classes.
@BsonDiscriminator(value = "email")
@RegisterForReflection
internal class EmailNotificationEntity @BsonCreator constructor(
    @BsonId @BsonProperty("_id") override val id: ObjectId,
    @BsonProperty("name") override val name: String,
    @BsonProperty("filter") override val filter: NotificationFilter,
    @BsonProperty("updatedAt") override val updatedAt: OffsetDateTime,
    @BsonProperty("createdAt") override val createdAt: OffsetDateTime,
    @BsonProperty("userId") override val userId: ObjectId,
    @BsonProperty("email") val email: String,
    @BsonProperty("enabled") override val enabled: Boolean,
) : NotificationEntity

@BsonDiscriminator(value = "api")
@RegisterForReflection
internal class WebhookNotificationEntity @BsonCreator constructor(
    @BsonId @BsonProperty("_id") override val id: ObjectId,
    @BsonProperty("name") override val name: String,
    @BsonProperty("filter") override val filter: NotificationFilter,
    @BsonProperty("updatedAt") override val updatedAt: OffsetDateTime,
    @BsonProperty("createdAt") override val createdAt: OffsetDateTime,
    @BsonProperty("userId") override val userId: ObjectId,
    @BsonProperty("url") val url: String,
    @BsonProperty("enabled") override val enabled: Boolean,
) : NotificationEntity

@BsonDiscriminator(value = "discord")
@RegisterForReflection
internal class DiscordWebhookNotificationEntity @BsonCreator constructor(
    @BsonId @BsonProperty("_id") override val id: ObjectId,
    @BsonProperty("name") override val name: String,
    @BsonProperty("filter") override val filter: NotificationFilter,
    @BsonProperty("updatedAt") override val updatedAt: OffsetDateTime,
    @BsonProperty("createdAt") override val createdAt: OffsetDateTime,
    @BsonProperty("userId") override val userId: ObjectId,
    @BsonProperty("webhookId") val webhookId: String,
    @BsonProperty("token") val token: String,
    @BsonProperty("enabled") override val enabled: Boolean,
) : NotificationEntity
