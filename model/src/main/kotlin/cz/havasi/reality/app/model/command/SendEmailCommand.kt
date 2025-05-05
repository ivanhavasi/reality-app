package cz.havasi.reality.app.model.command

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.EmailNotification

public data class SendEmailCommand(
    val notifications: List<EmailNotification>,
    val apartment: Apartment,
)
