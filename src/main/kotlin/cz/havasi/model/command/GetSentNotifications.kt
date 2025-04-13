package cz.havasi.model.command

import cz.havasi.model.enum.NotificationType
import cz.havasi.model.util.Paging

public data class GetSentNotifications(
    val userId: String,
    val paging: Paging,
    val apartmentId: String? = null,
    val notificationType: NotificationType? = null,
)
