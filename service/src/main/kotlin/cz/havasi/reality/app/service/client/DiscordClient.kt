package cz.havasi.reality.app.service.client

import cz.havasi.reality.app.model.command.SendApartmentDiscordMessageCommand

public interface DiscordClient {
    public suspend fun sendApartmentDiscordMessage(command: SendApartmentDiscordMessageCommand): String
}