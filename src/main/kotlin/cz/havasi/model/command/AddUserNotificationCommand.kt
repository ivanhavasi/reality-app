package cz.havasi.model.command

public data class AddUserNotificationCommand(
    val userId: String,
    val notification: AddNotificationCommand,
)
