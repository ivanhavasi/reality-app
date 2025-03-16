package cz.havasi.rest.controller

import cz.havasi.model.Notification
import cz.havasi.model.User
import cz.havasi.model.command.AddNotificationCommand
import cz.havasi.model.command.AddUserNotificationCommand
import cz.havasi.model.command.CreateUserCommand
import cz.havasi.model.command.RemoveUserNotificationCommand
import cz.havasi.rest.controller.model.ResponseId
import cz.havasi.service.NotificationService
import cz.havasi.service.UserService
import jakarta.annotation.security.RolesAllowed
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
internal open class UserController(
    private val userService: UserService,
    private val notificationService: NotificationService,
) {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    open suspend fun createUser(createUserCommand: CreateUserCommand): Response =
        userService
            .createUser(createUserCommand)
            .let { Response.ok(ResponseId(it)).build() }

    @GET
    @Path("/{id}")
    @RolesAllowed("USER")
    @Produces(MediaType.APPLICATION_JSON)
    open suspend fun getUserById(id: String): User =
        userService.getUserById(id)

    @POST
    @RolesAllowed("USER")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{userId}/notifications")
    open suspend fun addUserNotification(
        @PathParam("userId") userId: String,
        addNotificationCommand: AddNotificationCommand,
    ): Response =
        notificationService.addUserNotification(AddUserNotificationCommand(userId, addNotificationCommand))
            .let { Response.ok(ResponseId(it)).build() }

    @GET
    @RolesAllowed("USER")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{userId}/notifications")
    open suspend fun getUserNotifications(
        @PathParam("userId") userId: String,
    ): List<Notification> =
        notificationService
            .getUserNotifications(userId)

    @DELETE
    @RolesAllowed("USER")
    @Produces(MediaType.APPLICATION_JSON)
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
