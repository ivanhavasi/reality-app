package cz.havasi.controller

import cz.havasi.controller.model.ResponseId
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
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.ServerErrorException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/users")
@ApplicationScoped
internal class UserController(
    private val userService: UserService,
) {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    suspend fun createUser(createUserCommand: CreateUserCommand): Response =
        userService
            .createUser(createUserCommand)
            .let { Response.ok(ResponseId(it)).build() }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    suspend fun getUserById(id: String): User =
        userService.getUserById(id)

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{userId}/notifications")
    suspend fun addUserNotification(
        @PathParam("userId") userId: String,
        addNotificationCommand: AddNotificationCommand
    ): Response = // todo  maybe return notificationId
        userService.addUserNotification(AddUserNotificationCommand(userId, addNotificationCommand))
            .takeIf { it }
            ?.wrapToNoContent()
            ?: throw ServerErrorException("Notification was not added.", 500)

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{userId}/notifications/{notificationId}")
    suspend fun removeUserNotification(
        @PathParam("userId") userId: String,
        @PathParam("notificationId") notificationId: String,
    ): Response =
        userService
            .removeUserNotification(RemoveUserNotificationCommand(userId, notificationId))
            .takeIf { it }
            ?.wrapToNoContent()
            ?: throw ServerErrorException("Notification was not removed.", 500)


    private fun Boolean.wrapToNoContent() =
        Response.noContent().build()
}
