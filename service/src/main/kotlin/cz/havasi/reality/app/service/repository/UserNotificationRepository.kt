package cz.havasi.reality.app.service.repository

import cz.havasi.reality.app.model.Notification
import cz.havasi.reality.app.model.command.AddUserNotificationCommand
import cz.havasi.reality.app.model.command.FindUserNotificationsForFilterCommand
import cz.havasi.reality.app.model.command.UpdateUserNotificationCommand

public interface UserNotificationRepository {
    public suspend fun addUserNotification(command: AddUserNotificationCommand): String
    public suspend fun removeUserNotification(notificationId: String): Boolean
    public suspend fun updateUserNotification(command: UpdateUserNotificationCommand): Boolean

    public suspend fun getUserNotifications(userId: String): List<Notification>
    public suspend fun getUserNotificationById(notificationId: String): Notification
    public suspend fun findUserNotificationsForFilter(command: FindUserNotificationsForFilterCommand): List<Notification>
}
