package cz.havasi.model.event

import cz.havasi.model.Apartment
import cz.havasi.model.Notification

public data class NotificationEvent(
    val notifications: List<Notification>,
    val apartment: Apartment,
)
