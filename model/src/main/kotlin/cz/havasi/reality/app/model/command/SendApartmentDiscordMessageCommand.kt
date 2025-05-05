package cz.havasi.reality.app.model.command

import cz.havasi.reality.app.model.Apartment

public data class SendApartmentDiscordMessageCommand(
    val webhookId: String,
    val webhookToken: String,
    val apartment: Apartment,
)
