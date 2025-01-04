package cz.havasi.model.command

import cz.havasi.model.Apartment
import cz.havasi.model.Notification

public data class HandleNotificationsCommand<T: Notification>(
    val apartment: Apartment,
    val notifications: List<T>
)
