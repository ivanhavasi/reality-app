package cz.havasi.rest.controller

import cz.havasi.config.security.RequireUserMatch
import cz.havasi.model.Notification
import cz.havasi.model.User
import cz.havasi.model.command.AddNotificationCommand
import cz.havasi.model.command.AddUserNotificationCommand
import cz.havasi.model.command.CreateUserCommand
import cz.havasi.model.command.RemoveUserNotificationCommand
import cz.havasi.rest.controller.model.ResponseId
import cz.havasi.rest.controller.util.wrapToNoContent
import cz.havasi.rest.controller.util.wrapToOk
import cz.havasi.service.NotificationService
import cz.havasi.service.UserService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import org.jboss.resteasy.reactive.RestResponse

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
internal open class UserController(
    private val userService: UserService,
    private val notificationService: NotificationService,
) {
    // shouldn't be called
    open suspend fun createUser(createUserCommand: CreateUserCommand): RestResponse<ResponseId> =
        userService
            .createUser(createUserCommand)
            .let { ResponseId(it).wrapToOk() }

    @GET
    @Path("/{userId}")
    @RequireUserMatch
    open suspend fun getUserById(@PathParam("userId") userId: String): RestResponse<User> =
        userService
            .getUserById(userId)
            .wrapToOk()

    @POST
    @RequireUserMatch
    @Path("/{userId}/notifications")
    open suspend fun addUserNotification(
        @PathParam("userId") userId: String,
        addNotificationCommand: AddNotificationCommand,
    ): RestResponse<ResponseId> =
        notificationService
            .addUserNotification(AddUserNotificationCommand(userId, addNotificationCommand))
            .let { ResponseId(it).wrapToOk() }

    @GET
    @RequireUserMatch
    @Path("/{userId}/notifications")
    open suspend fun getUserNotifications(
        @PathParam("userId") userId: String,
    ): RestResponse<List<Notification>> =
        notificationService
            .getUserNotifications(userId)
            .wrapToOk()

    @DELETE
    @RequireUserMatch
    @Path("/{userId}/notifications/{notificationId}")
    open suspend fun removeUserNotification(
        @PathParam("userId") userId: String,
        @PathParam("notificationId") notificationId: String,
    ): RestResponse<Nothing> =
        notificationService
            .removeUserNotification(RemoveUserNotificationCommand(userId, notificationId))
            .takeIf { it }
            ?.wrapToNoContent()
            ?: throw ServerErrorException("Notification was not removed.", 500)
}
