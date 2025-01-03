package cz.havasi.service

import cz.havasi.model.Notification
import cz.havasi.model.command.AddUserNotificationCommand
import cz.havasi.model.command.RemoveUserNotificationCommand
import cz.havasi.repository.NotificationRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
public class NotificationService(
    private val notificationRepository: NotificationRepository,
) {
    public suspend fun addUserNotification(addUserNotificationCommand: AddUserNotificationCommand): String =
        notificationRepository.addUserNotification(addUserNotificationCommand)

    public suspend fun removeUserNotification(removeUserNotificationCommand: RemoveUserNotificationCommand): Boolean =
        notificationRepository.removeUserNotification(removeUserNotificationCommand)

    public suspend fun getUserNotifications(userId: String): List<Notification> =
        notificationRepository.getUserNotifications(userId)
}
