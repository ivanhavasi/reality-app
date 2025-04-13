package cz.havasi.model

import cz.havasi.model.enum.NotificationType
import cz.havasi.model.enum.ProviderType
import java.time.OffsetDateTime

public data class SentNotification(
    val notificationId: String,
    val userId: String,
    val type: NotificationType,
    val realEstate: SentNotificationRealEstate,
    val sentAt: OffsetDateTime,
)

public data class SentNotificationRealEstate(
    val id: String,
    val name: String,
    val url: String,
    val price: Double,
    val city: String,
    val image: String,
    val provider: ProviderType,
)
