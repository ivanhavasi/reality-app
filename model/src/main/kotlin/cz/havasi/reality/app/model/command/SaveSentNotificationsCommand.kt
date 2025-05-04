package cz.havasi.reality.app.model.command

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.Notification

public data class SaveSentNotificationsCommand(
    val notifications: List<Notification>,
    val apartment: Apartment,
)
