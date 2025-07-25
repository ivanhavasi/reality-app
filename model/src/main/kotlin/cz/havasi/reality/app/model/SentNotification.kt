package cz.havasi.reality.app.model

import cz.havasi.reality.app.model.type.NotificationType
import cz.havasi.reality.app.model.type.ProviderType
import io.quarkus.runtime.annotations.RegisterForReflection
import java.time.OffsetDateTime

@RegisterForReflection
public data class SentNotification(
    val notificationId: String,
    val userId: String,
    val type: NotificationType,
    val realEstate: SentNotificationRealEstate,
    val sentAt: OffsetDateTime,
)

@RegisterForReflection
public data class SentNotificationRealEstate(
    val id: String,
    val name: String,
    val url: String,
    val price: Double,
    val city: String,
    val image: String,
    val provider: ProviderType,
)
