package cz.havasi.reality.app.service.notification

import cz.havasi.reality.app.model.Notification
import cz.havasi.reality.app.model.event.HandleNotificationsEvent

public interface NotificationEventHandler<T : Notification> {
    public fun handleNotifications(event: HandleNotificationsEvent<T>)
}
