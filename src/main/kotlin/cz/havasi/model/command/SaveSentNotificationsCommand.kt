package cz.havasi.model.command

import cz.havasi.model.Apartment
import cz.havasi.model.Notification

public data class SaveSentNotificationsCommand(
    val notifications: List<Notification>,
    val apartment: Apartment,
)
