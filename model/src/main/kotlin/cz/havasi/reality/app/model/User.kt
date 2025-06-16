package cz.havasi.reality.app.model

import cz.havasi.reality.app.model.type.UserRole
import io.quarkus.runtime.annotations.RegisterForReflection
import java.time.OffsetDateTime

@RegisterForReflection
public data class User(
    val id: String,
    val email: String,
    val googleId: String,
    val roles: Set<UserRole>,
    val username: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
