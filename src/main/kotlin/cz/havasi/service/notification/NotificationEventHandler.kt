package cz.havasi.service.notification

import cz.havasi.model.Notification
import cz.havasi.model.event.HandleNotificationsEvent

public interface NotificationEventHandler<T : Notification> {
    public fun handleNotifications(event: HandleNotificationsEvent<T>)
}
