package cz.havasi.reality.app.model.command

public data class UpdateUserNotificationCommand(
    val userId: String,
    val notificationId: String,
    val enabled: Boolean? = null,
)
