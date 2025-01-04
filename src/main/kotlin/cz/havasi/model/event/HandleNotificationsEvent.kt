package cz.havasi.model.event

import cz.havasi.model.Apartment
import cz.havasi.model.Notification

public data class HandleNotificationsEvent<T : Notification>(
    val apartment: Apartment,
    val notifications: List<T>,
)
