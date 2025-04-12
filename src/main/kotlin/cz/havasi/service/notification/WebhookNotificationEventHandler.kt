package cz.havasi.service.notification

import cz.havasi.model.WebhookNotification
import cz.havasi.model.event.HandleNotificationsEvent
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ApplicationScoped
internal class WebhookNotificationEventHandler(
    // @RestClient
) : NotificationEventHandler<WebhookNotification> {

    override fun handleNotifications(@Observes event: HandleNotificationsEvent<WebhookNotification>) {
        CoroutineScope(Dispatchers.IO).launch {
            event.sendWebhooks()
        }
    }

    private suspend fun HandleNotificationsEvent<WebhookNotification>.sendWebhooks(): Unit {
        Log.error("Webhook notification sender is not implemented yet!")
        // save sent notification
    }
}
