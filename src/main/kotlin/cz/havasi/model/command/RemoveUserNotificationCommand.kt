package cz.havasi.model.command

public data class RemoveUserNotificationCommand(
    val userId: String,
    val notificationId: String,
)
