package cz.havasi.service

import cz.havasi.model.SentNotification
import cz.havasi.model.command.GetSentNotifications
import cz.havasi.repository.SentNotificationRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
public class SentNotificationService(
    private val sentNotificationRepository: SentNotificationRepository,
) {
    public suspend fun getSentNotifications(command: GetSentNotifications): List<SentNotification> =
        sentNotificationRepository.getSentNotifications(command)
}