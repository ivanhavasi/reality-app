package cz.havasi.repository

import cz.havasi.model.command.SaveSentNotificationsCommand

public interface SentNotificationRepository {
    public suspend fun saveSentNotifications(saveSentNotificationsCommand: SaveSentNotificationsCommand): Unit
}