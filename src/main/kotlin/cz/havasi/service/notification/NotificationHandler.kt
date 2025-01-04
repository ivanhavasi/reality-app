package cz.havasi.service.notification

import cz.havasi.model.Notification
import cz.havasi.model.command.HandleNotificationsCommand

public fun interface NotificationHandler<T : Notification> {
    public fun handleNotifications(command: HandleNotificationsCommand<T>)
}
