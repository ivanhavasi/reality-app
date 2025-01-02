package cz.havasi.model.command

public data class CreateUserCommand(
    val email: String,
    val username: String,
)
