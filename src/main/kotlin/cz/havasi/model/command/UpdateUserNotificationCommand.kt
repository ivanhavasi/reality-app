package cz.havasi.model.command

public data class UpdateUserNotificationCommand(
    val notificationId: String,
    val enabled: Boolean? = null,
)
