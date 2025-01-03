package cz.havasi.repository

import cz.havasi.model.Notification
import cz.havasi.model.command.AddUserNotificationCommand
import cz.havasi.model.command.FindNotificationsForFilterCommand
import cz.havasi.model.command.RemoveUserNotificationCommand

public interface NotificationRepository {
    public suspend fun addUserNotification(command: AddUserNotificationCommand): String
    public suspend fun removeUserNotification(command: RemoveUserNotificationCommand): Boolean

    public suspend fun getUserNotifications(userId: String): List<Notification>
    public suspend fun getNotificationById(notificationId: String): Notification
    public suspend fun findNotificationsForFilter(command: FindNotificationsForFilterCommand): List<Notification>
}
