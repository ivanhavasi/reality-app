package cz.havasi.reality.app.mongo.entity

import io.quarkus.runtime.annotations.RegisterForReflection
import java.time.OffsetDateTime

@RegisterForReflection
internal data class GoogleRefreshTokenEntity(
    val _id: String, // the user's email
    val refreshToken: String,
    val updatedAt: OffsetDateTime,
)
