package cz.havasi.reality.app.model.command

import cz.havasi.reality.app.model.type.NotificationType
import cz.havasi.reality.app.model.util.Paging

public data class GetSentNotifications(
    val userId: String,
    val paging: Paging,
    val apartmentId: String? = null,
    val notificationType: NotificationType? = null,
)
