package cz.havasi.service

import cz.havasi.model.command.AddNotificationCommand
import cz.havasi.model.command.AddUserNotificationCommand
import cz.havasi.model.command.CreateUserCommand
import cz.havasi.model.command.RemoveUserNotificationCommand
import cz.havasi.repository.UserRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
internal class UserService(
    private val userRepository: UserRepository,
) {
    suspend fun createUser(createUserCommand: CreateUserCommand): String =
        userRepository.save(createUserCommand)

    suspend fun getUserById(id: String) =
        userRepository.getUserById(id)

    suspend fun addUserNotification(addUserNotificationCommand: AddUserNotificationCommand) =
        userRepository.addUserNotification(addUserNotificationCommand)

    suspend fun removeUserNotification(removeUserNotificationCommand: RemoveUserNotificationCommand) =
        userRepository.removeUserNotification(removeUserNotificationCommand)
}
