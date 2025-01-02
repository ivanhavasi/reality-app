package cz.havasi.model.command

import cz.havasi.model.Notification

public data class AddUserNotificationCommand(
    val userId: String,
    val notification: Notification,
)
