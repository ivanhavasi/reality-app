package cz.havasi.model.command

import cz.havasi.model.enum.UserRole

public data class CreateUserCommand(
    val email: String,
    val username: String,
    val googleId: String,
    val roles: List<UserRole> = listOf(UserRole.USER),
)
