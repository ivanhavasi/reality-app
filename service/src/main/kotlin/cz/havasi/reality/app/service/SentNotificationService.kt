package cz.havasi.reality.app.service

import cz.havasi.reality.app.model.SentNotification
import cz.havasi.reality.app.model.command.GetSentNotifications
import cz.havasi.reality.app.service.repository.SentNotificationRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
public class SentNotificationService(
    private val sentNotificationRepository: SentNotificationRepository,
) {
    public suspend fun getSentNotifications(command: GetSentNotifications): List<SentNotification> =
        sentNotificationRepository.getSentNotifications(command)
}