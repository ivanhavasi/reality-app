package cz.havasi.service

import cz.havasi.model.Apartment
import cz.havasi.model.DiscordWebhookNotification
import cz.havasi.model.EmailNotification
import cz.havasi.model.Notification
import cz.havasi.model.WebhookNotification
import cz.havasi.model.command.AddUserNotificationCommand
import cz.havasi.model.command.FindNotificationsForFilterCommand
import cz.havasi.model.command.RemoveUserNotificationCommand
import cz.havasi.model.event.HandleNotificationsEvent
import cz.havasi.repository.NotificationRepository
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Event

@ApplicationScoped
public class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val emailNotificationSender: Event<HandleNotificationsEvent<EmailNotification>>,
    private val webhookNotificationSender: Event<HandleNotificationsEvent<WebhookNotification>>,
    private val discordWebhookNotificationSender: Event<HandleNotificationsEvent<DiscordWebhookNotification>>,
) {
    public suspend fun sendNotificationsForApartments(apartments: List<Apartment>) {
        apartments.forEach { apartment ->
            apartment
                .getValidNotifications()
                .sendNotifications(apartment)
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

    private fun List<Notification>.sendNotifications(apartment: Apartment) {
        val emailNotifications = mutableListOf<EmailNotification>()
        val webhookNotifications = mutableListOf<WebhookNotification>()
        val discordNotifications = mutableListOf<DiscordWebhookNotification>()

        // sort notifications into correct lists
        forEach { notification ->
            when (notification) {
                is EmailNotification -> emailNotifications.add(notification)
                is WebhookNotification -> webhookNotifications.add(notification)
                is DiscordWebhookNotification -> discordNotifications.add(notification)
            }
        }

        Log.debug("Firing event for notifications for apartment ${apartment.id}")
        // fire events for notifications
        if (emailNotifications.isNotEmpty()) {
            emailNotificationSender.fire(HandleNotificationsEvent(apartment, emailNotifications))
        }
        if (webhookNotifications.isNotEmpty()) {
            webhookNotificationSender.fire(HandleNotificationsEvent(apartment, webhookNotifications))
        }
        if (discordNotifications.isNotEmpty()) {
            discordWebhookNotificationSender.fire(HandleNotificationsEvent(apartment, discordNotifications))
        }
    }

    private fun Apartment.toFilterCommand() =
        FindNotificationsForFilterCommand(
            buildingType = mainCategory,
            transactionType = transactionType,
            price = price.toInt(),
            size = sizeInM2,
            subTypes = if (subCategory != null) listOf(subCategory) else emptyList(),
        )
}
