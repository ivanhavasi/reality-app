package cz.havasi.reality.app.model.command

public data class UpdateUserNotificationCommand(
    val notificationId: String,
    val enabled: Boolean? = null,
)
