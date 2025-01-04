package cz.havasi.service.notification

import cz.havasi.model.WebhookNotification
import cz.havasi.model.command.HandleNotificationsCommand
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ApplicationScoped
internal class WebhookNotificationHandler(
    // @RestClient
) : NotificationHandler<WebhookNotification> {

    override fun handleNotifications(command: HandleNotificationsCommand<WebhookNotification>) {
        CoroutineScope(Dispatchers.IO).launch {
            command.sendWebhooks()
        }
    }

    private suspend fun HandleNotificationsCommand<WebhookNotification>.sendWebhooks(): Unit {
        Log.error("Webhook notification sender is not implemented yet!")
    }
}
