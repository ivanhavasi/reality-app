package cz.havasi.reality.app.service.notification

import cz.havasi.reality.app.model.EmailNotification
import cz.havasi.reality.app.model.command.SaveSentNotificationsCommand
import cz.havasi.reality.app.model.command.SendEmailCommand
import cz.havasi.reality.app.model.event.HandleNotificationsEvent
import cz.havasi.reality.app.service.client.EmailClient
import cz.havasi.reality.app.service.repository.SentNotificationRepository
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ApplicationScoped
internal class EmailNotificationEventHandler(
    private val emailClient: EmailClient,
    private val sentNotificationRepository: SentNotificationRepository,
) : NotificationEventHandler<EmailNotification> {

    override fun handleNotifications(@Observes event: HandleNotificationsEvent<EmailNotification>) {
        Log.debug("Handling ${event.notifications.size} email notifications for apartment ${event.apartment.id}")
        CoroutineScope(Dispatchers.IO).launch {
            event.sendEmails()
        }
    }

    private suspend fun HandleNotificationsEvent<EmailNotification>.sendEmails(): Unit {
        try {
            emailClient.sendEmail(
                SendEmailCommand(
                    notifications = notifications,
                    apartment = apartment,
                ),
            )
            sentNotificationRepository.saveSentNotifications(
                SaveSentNotificationsCommand(
                    notifications = notifications,
                    apartment = apartment,
                ),
            )
        } catch (e: Exception) {
            Log.error("Error sending emails", e)
            throw e
        }
    }
}
