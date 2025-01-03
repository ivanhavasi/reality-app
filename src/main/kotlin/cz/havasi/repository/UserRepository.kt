package cz.havasi.repository

import cz.havasi.model.User
import cz.havasi.model.command.CreateUserCommand

public interface UserRepository {
    public suspend fun save(command: CreateUserCommand): String
    public suspend fun getUserById(id: String): User
}
