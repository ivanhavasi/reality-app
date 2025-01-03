package cz.havasi.model

import java.time.OffsetDateTime

public data class User(
    val id: String,
    val email: String,
    val username: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
