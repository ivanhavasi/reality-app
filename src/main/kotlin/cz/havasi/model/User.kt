package cz.havasi.model

import java.time.OffsetDateTime

public data class User(
    val id: String,
    val email: String,
    val username: String,
    val notifications: List<Notification>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
