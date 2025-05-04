package cz.havasi.reality.app.service.repository

import cz.havasi.reality.app.model.SentNotification
import cz.havasi.reality.app.model.command.GetSentNotifications
import cz.havasi.reality.app.model.command.SaveSentNotificationsCommand

public interface SentNotificationRepository {
    public suspend fun saveSentNotifications(command: SaveSentNotificationsCommand)
    public suspend fun getSentNotifications(command: GetSentNotifications): List<SentNotification>
}
