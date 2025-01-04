package cz.havasi.service

import cz.havasi.model.Apartment
import cz.havasi.model.Notification
import cz.havasi.model.command.AddUserNotificationCommand
import cz.havasi.model.command.FindNotificationsForFilterCommand
import cz.havasi.model.command.RemoveUserNotificationCommand
import cz.havasi.model.event.NotificationEvent
import cz.havasi.repository.NotificationRepository
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Event

@ApplicationScoped
public class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val eventSender: Event<NotificationEvent<Notification>>,
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
        Log.debug("Firing async event for notifications for apartment ${apartment.id}")
        val event = NotificationEvent(this, apartment)
        eventSender.fireAsync(event)
    }

    private fun Apartment.toFilterCommand() =
        FindNotificationsForFilterCommand(
            buildingType = mainCategory,
            transactionType = transactionType,
            price = price.toInt(),
            size = sizeInM2,
        )
}
