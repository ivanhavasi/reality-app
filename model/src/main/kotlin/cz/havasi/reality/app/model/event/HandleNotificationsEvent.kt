package cz.havasi.reality.app.model.event

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.Notification

public data class HandleNotificationsEvent<T : Notification>(
    val apartment: Apartment,
    val notifications: List<T>,
)
