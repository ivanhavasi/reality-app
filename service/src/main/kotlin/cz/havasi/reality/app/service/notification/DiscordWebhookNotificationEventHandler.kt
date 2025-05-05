package cz.havasi.reality.app.service.notification

import cz.havasi.reality.app.model.DiscordWebhookNotification
import cz.havasi.reality.app.model.command.SaveSentNotificationsCommand
import cz.havasi.reality.app.model.command.SendApartmentDiscordMessageCommand
import cz.havasi.reality.app.model.event.HandleNotificationsEvent
import cz.havasi.reality.app.service.client.DiscordClient
import cz.havasi.reality.app.service.repository.SentNotificationRepository
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ApplicationScoped
internal class DiscordWebhookNotificationEventHandler(
    private val discordClient: DiscordClient,
    private val sentNotificationRepository: SentNotificationRepository,
) : NotificationEventHandler<DiscordWebhookNotification> {

    override fun handleNotifications(@Observes event: HandleNotificationsEvent<DiscordWebhookNotification>) {
        Log.info("Handling ${event.notifications.size} discord notifications for apartment ${event.apartment.id}")
        CoroutineScope(Dispatchers.IO).launch {
            event.sendWebhooks()
        }
    }

    private suspend fun HandleNotificationsEvent<DiscordWebhookNotification>.sendWebhooks() {
        notifications.forEach { notification ->
            try {
                discordClient.sendApartmentDiscordMessage(
                    SendApartmentDiscordMessageCommand(
                        webhookId = notification.webhookId,
                        webhookToken = notification.token,
                        apartment = apartment,
                    ),
                )
                sentNotificationRepository.saveSentNotifications(
                    SaveSentNotificationsCommand(
                        notifications = listOf(notification),
                        apartment = apartment,
                    ),
                )
            } catch (e: Exception) {
                Log.error("Error sending discord webhooks for notification ${notification.id}", e)
                throw e
            }
        }
    }
}
