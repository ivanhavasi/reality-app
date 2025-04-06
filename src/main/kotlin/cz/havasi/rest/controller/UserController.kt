package cz.havasi.rest.controller

import cz.havasi.config.security.RequireUserMatch
import cz.havasi.model.Notification
import cz.havasi.model.User
import cz.havasi.model.command.AddNotificationCommand
import cz.havasi.model.command.AddUserNotificationCommand
import cz.havasi.model.command.CreateUserCommand
import cz.havasi.model.command.RemoveUserNotificationCommand
import cz.havasi.rest.controller.model.ResponseId
import cz.havasi.service.NotificationService
import cz.havasi.service.UserService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
internal open class UserController(
    private val userService: UserService,
    private val notificationService: NotificationService,
) {
    // shouldn't be called
    open suspend fun createUser(createUserCommand: CreateUserCommand): Response =
        userService
            .createUser(createUserCommand)
            .let { Response.ok(ResponseId(it)).build() }

    @GET
    @Path("/{userId}")
    @RequireUserMatch
    open suspend fun getUserById(@PathParam("userId") userId: String): User =
        userService.getUserById(userId)

    @POST
    @RequireUserMatch
    @Path("/{userId}/notifications")
    open suspend fun addUserNotification(
        @PathParam("userId") userId: String,
        addNotificationCommand: AddNotificationCommand,
    ): Response =
        notificationService.addUserNotification(AddUserNotificationCommand(userId, addNotificationCommand))
            .let { Response.ok(ResponseId(it)).build() }

    @GET
    @RequireUserMatch
    @Path("/{userId}/notifications")
    open suspend fun getUserNotifications(
        @PathParam("userId") userId: String,
    ): List<Notification> =
        notificationService
            .getUserNotifications(userId)

    @DELETE
    @RequireUserMatch
    @Path("/{userId}/notifications/{notificationId}")
    open suspend fun removeUserNotification(
        @PathParam("userId") userId: String,
        @PathParam("notificationId") notificationId: String,
    ): Response =
        notificationService
            .removeUserNotification(RemoveUserNotificationCommand(userId, notificationId))
            .takeIf { it }
            ?.wrapToNoContent()
            ?: throw ServerErrorException("Notification was not removed.", 500)


    private fun Boolean.wrapToNoContent() =
        Response.noContent().build()
}
