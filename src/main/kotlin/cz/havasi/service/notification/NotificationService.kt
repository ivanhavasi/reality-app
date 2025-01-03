package cz.havasi.service.notification

import cz.havasi.model.Apartment
import cz.havasi.model.EmailNotification
import cz.havasi.model.Notification
import cz.havasi.model.WebhookNotification
import cz.havasi.model.command.AddUserNotificationCommand
import cz.havasi.model.command.FindNotificationsForFilterCommand
import cz.havasi.model.command.RemoveUserNotificationCommand
import cz.havasi.repository.NotificationRepository
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
public class NotificationService(
    private val notificationRepository: NotificationRepository,
) {
    public suspend fun sendNotificationsForApartments(apartments: List<Apartment>) {
        apartments.forEach { apartment ->
            apartment
                .getValidNotifications()
                .sendNotifications()
        }
    }

    public suspend fun addUserNotification(addUserNotificationCommand: AddUserNotificationCommand): String =
        notificationRepository.addUserNotification(addUserNotificationCommand)

    public suspend fun removeUserNotification(removeUserNotificationCommand: RemoveUserNotificationCommand): Boolean =
        notificationRepository.removeUserNotification(removeUserNotificationCommand)

    public suspend fun getUserNotifications(userId: String): List<Notification> =
        notificationRepository.getUserNotifications(userId)

    private suspend fun Apartment.getValidNotifications() =
        notificationRepository.findNotificationsForFilter(toFilterCommand())
            .also {
                Log.info("Found ${it.size} notifications for apartment ${this.id}")
            }

    private suspend fun List<Notification>.sendNotifications() {
        forEach { notification ->
            when (notification) {
                is EmailNotification -> {
                    // send email
                    Log.warn("Email notification not implemented yet!")
                }
                is WebhookNotification -> {
                    // send webhook
                    Log.warn("Webhook notification not implemented yet!")
                }
            }
        }
    }

    private fun Apartment.toFilterCommand() =
        FindNotificationsForFilterCommand(
            buildingType = mainCategory,
            transactionType = transactionType,
            price = price.toInt(),
            size = sizeInM2,
        )
}
