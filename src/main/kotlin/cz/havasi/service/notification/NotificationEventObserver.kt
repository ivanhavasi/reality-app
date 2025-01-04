package cz.havasi.service.notification

import cz.havasi.model.EmailNotification
import cz.havasi.model.WebhookNotification
import cz.havasi.model.command.HandleNotificationsCommand
import cz.havasi.model.event.NotificationEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes

@ApplicationScoped
public class NotificationEventObserver internal constructor(
    private val emailNotificationHandler: EmailNotificationHandler,
    private val webhookNotificationHandler: WebhookNotificationHandler,
) {

    public fun observeNotifications(@Observes event: NotificationEvent) { // todo maybe this should be done in notification service and this class shouldnt exist
        event.handleNotificationEvent()
    }

    private fun NotificationEvent.handleNotificationEvent() {
        val emailNotifications = mutableListOf<EmailNotification>()
        val webhookNotifications = mutableListOf<WebhookNotification>()

        notifications.forEach { notification ->
            when (notification) {
                is EmailNotification -> emailNotifications.add(notification)
                is WebhookNotification -> webhookNotifications.add(notification)
            }
        }
        emailNotificationHandler.handleNotifications(HandleNotificationsCommand(apartment, emailNotifications))
        webhookNotificationHandler.handleNotifications(HandleNotificationsCommand(apartment, webhookNotifications))
    }
}
