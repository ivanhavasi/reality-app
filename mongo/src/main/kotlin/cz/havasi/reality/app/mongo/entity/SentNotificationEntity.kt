package cz.havasi.reality.app.mongo.entity

import cz.havasi.reality.app.mongo.entity.type.NotificationTypeEntity
import cz.havasi.reality.app.mongo.entity.type.ProviderTypeEntity
import io.quarkus.runtime.annotations.RegisterForReflection
import org.bson.types.ObjectId
import java.time.OffsetDateTime

@RegisterForReflection
internal data class SentNotificationEntity(
    val _id: ObjectId,
    val notificationId: ObjectId,
    val userId: ObjectId,
    val type: NotificationTypeEntity,
    val realEstate: SentNotificationRealEstateEntity,
    val sentAt: OffsetDateTime,
)

@RegisterForReflection
internal data class SentNotificationRealEstateEntity(
    val id: String,
    val name: String,
    val url: String,
    val price: Double,
    val city: String,
    val image: String,
    val provider: ProviderTypeEntity = ProviderTypeEntity.UNKNOWN,
)
