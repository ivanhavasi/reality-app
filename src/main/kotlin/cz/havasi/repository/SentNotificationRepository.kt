package cz.havasi.repository

import cz.havasi.model.SentNotification
import cz.havasi.model.command.GetSentNotifications
import cz.havasi.model.command.SaveSentNotificationsCommand

public interface SentNotificationRepository {
    public suspend fun saveSentNotifications(command: SaveSentNotificationsCommand): Unit
    public suspend fun getSentNotifications(command: GetSentNotifications): List<SentNotification>
}