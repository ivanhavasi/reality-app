package cz.havasi.service

import cz.havasi.model.User
import cz.havasi.model.command.CreateUserCommand
import cz.havasi.repository.UserRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
public class UserService(
    private val userRepository: UserRepository,
) {
    public suspend fun createUser(createUserCommand: CreateUserCommand): String =
        userRepository.save(createUserCommand)

    public suspend fun getUserById(id: String): User =
        userRepository.getUserById(id)
}
