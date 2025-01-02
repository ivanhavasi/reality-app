package cz.havasi.controller

import cz.havasi.model.User
import cz.havasi.model.command.AddNotificationCommand
import cz.havasi.model.command.AddUserNotificationCommand
import cz.havasi.model.command.CreateUserCommand
import cz.havasi.model.command.RemoveUserNotificationCommand
import cz.havasi.service.UserService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/users")
@ApplicationScoped
internal class UserController(
    private val userService: UserService,
) {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    suspend fun createUser(createUserCommand: CreateUserCommand): String =
        // todo wrap these responses in a response object
        userService.createUser(createUserCommand)

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun getUserById(id: String): User =
        userService.getUserById(id)

    @POST
    @Path("/{userId}/notifications")
    suspend fun addUserNotification(userId: String, addNotificationCommand: AddNotificationCommand): Boolean {
        // todo wrap these responses in a response object
        println("HEREEE")
        println(userId)
        return userService.addUserNotification(AddUserNotificationCommand(userId, addNotificationCommand))
    }

    @DELETE
    @Path("/{userId}/notifications/{notificationId}")
    suspend fun removeUserNotification(userId: String, notificationId: String): Boolean =
        // todo wrap these responses in a response object
        userService.removeUserNotification(RemoveUserNotificationCommand(userId, notificationId))
}
