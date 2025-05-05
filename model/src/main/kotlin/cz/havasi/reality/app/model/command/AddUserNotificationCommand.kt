package cz.havasi.reality.app.model.command

public data class AddUserNotificationCommand(
    val userId: String,
    val notification: AddNotificationCommand,
)
