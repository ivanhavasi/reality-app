package cz.havasi.reality.app.service.repository

import cz.havasi.reality.app.model.User
import cz.havasi.reality.app.model.command.CreateUserCommand

public interface UserRepository {
    public suspend fun save(command: CreateUserCommand): String
    public suspend fun getUserById(id: String): User
    public suspend fun getUserByEmailOrNull(email: String): User?
}
