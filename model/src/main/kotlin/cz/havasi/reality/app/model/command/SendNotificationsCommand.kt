package cz.havasi.reality.app.model.command

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.Notification

public data class SendNotificationsCommand<T : Notification>(
    val notifications: List<T>,
    val apartment: Apartment,
)
