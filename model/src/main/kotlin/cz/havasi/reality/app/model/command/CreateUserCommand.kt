package cz.havasi.reality.app.model.command

import cz.havasi.reality.app.model.type.UserRole

public data class CreateUserCommand(
    val email: String,
    val username: String,
    val googleId: String,
    val roles: List<UserRole> = listOf(UserRole.USER),
)
