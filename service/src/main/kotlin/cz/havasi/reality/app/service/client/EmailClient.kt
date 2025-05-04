package cz.havasi.reality.app.service.client

import cz.havasi.reality.app.model.command.SendEmailCommand

public interface EmailClient {
    public suspend fun sendEmail(command: SendEmailCommand): Unit
}