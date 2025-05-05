package cz.havasi.reality.app.service

import cz.havasi.reality.app.model.User
import cz.havasi.reality.app.model.command.CreateUserCommand
import cz.havasi.reality.app.service.repository.UserRepository
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
