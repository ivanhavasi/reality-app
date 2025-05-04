package cz.havasi.reality.app.service

import cz.havasi.reality.app.model.*
import cz.havasi.reality.app.model.command.AddUserNotificationCommand
import cz.havasi.reality.app.model.command.FindUserNotificationsForFilterCommand
import cz.havasi.reality.app.model.command.UpdateUserNotificationCommand
import cz.havasi.reality.app.model.event.HandleNotificationsEvent
import cz.havasi.reality.app.service.repository.UserNotificationRepository
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Event

@ApplicationScoped
public class UserNotificationService(
    private val userNotificationRepository: UserNotificationRepository,
    private val emailNotificationSender: Event<HandleNotificationsEvent<EmailNotification>>,
    private val webhookNotificationSender: Event<HandleNotificationsEvent<WebhookNotification>>,
    private val discordWebhookNotificationSender: Event<HandleNotificationsEvent<DiscordWebhookNotification>>,
) {
    public suspend fun sendUserNotificationsForApartments(apartments: List<Apartment>) {
        apartments.forEach { apartment ->
            apartment
                .getValidUserNotifications()
                .sendUserNotifications(apartment)
        }
    }

    public suspend fun addUserNotification(addUserNotificationCommand: AddUserNotificationCommand): String =
        userNotificationRepository.addUserNotification(addUserNotificationCommand)

    public suspend fun removeUserNotification(notificationId: String): Boolean =
        userNotificationRepository.removeUserNotification(notificationId)

    public suspend fun getUserNotifications(userId: String): List<Notification> =
        userNotificationRepository.getUserNotifications(userId)

    public suspend fun enableUserNotification(notificationId: String): Boolean =
        userNotificationRepository.updateUserNotification(
            UpdateUserNotificationCommand(
                notificationId = notificationId,
                enabled = true,
            ),
        )

    public suspend fun disableUserNotification(notificationId: String): Boolean =
        userNotificationRepository.updateUserNotification(
            UpdateUserNotificationCommand(
                notificationId = notificationId,
                enabled = false,
            ),
        )

    private suspend fun Apartment.getValidUserNotifications() =
        userNotificationRepository.findUserNotificationsForFilter(toFilterCommand())
            .also {
                Log.info("Found ${it.size} notifications for apartment ${this.id}")
            }

    private fun List<Notification>.sendUserNotifications(apartment: Apartment) {
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
        FindUserNotificationsForFilterCommand(
            buildingType = mainCategory,
            transactionType = transactionType,
            price = price.toInt(),
            size = sizeInM2,
            subTypes = if (subCategory != null) listOf(subCategory!!) else emptyList(),
        )
}
