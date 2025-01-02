package cz.havasi.repository

import cz.havasi.model.User
import cz.havasi.model.command.AddUserNotificationCommand
import cz.havasi.model.command.CreateUserCommand
import cz.havasi.model.command.RemoveUserNotificationCommand

public interface UserRepository {
    public suspend fun save(command: CreateUserCommand): String
    public suspend fun getUserById(id: String): User
    public suspend fun addUserNotification(command: AddUserNotificationCommand): Boolean
    public suspend fun removeUserNotification(command: RemoveUserNotificationCommand): Boolean
}
