package cz.havasi.model.command

import cz.havasi.model.Apartment
import cz.havasi.model.Notification

public data class SendNotificationsCommand<T : Notification>(
    val notifications: List<T>,
    val apartment: Apartment,
)
