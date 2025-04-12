package cz.havasi.repository.entity

import cz.havasi.repository.entity.enum.ProviderTypeEntity
import io.quarkus.runtime.annotations.RegisterForReflection
import org.bson.types.ObjectId
import java.time.OffsetDateTime

@RegisterForReflection
internal data class SentNotificationEntity(
    val _id: ObjectId,
    val notificationId: ObjectId,
    val userId: ObjectId,
    val type: String, // discord, webhook, email
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
