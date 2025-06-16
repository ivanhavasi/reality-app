package cz.havasi.reality.app.model.command

import cz.havasi.reality.app.model.type.UserRole
import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
public data class CreateUserCommand(
    val email: String,
    val username: String,
    val googleId: String,
    val roles: List<UserRole> = listOf(UserRole.USER),
)
