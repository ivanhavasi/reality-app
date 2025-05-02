package cz.havasi.rest.controller

import cz.havasi.config.security.RequireUserMatch
import cz.havasi.model.Notification
import cz.havasi.model.SentNotification
import cz.havasi.model.User
import cz.havasi.model.command.AddNotificationCommand
import cz.havasi.model.command.AddUserNotificationCommand
import cz.havasi.model.command.CreateUserCommand
import cz.havasi.model.command.GetSentNotifications
import cz.havasi.model.enum.NotificationType
import cz.havasi.model.util.Paging
import cz.havasi.rest.controller.model.ResponseId
import cz.havasi.rest.controller.util.wrapToNoContent
import cz.havasi.rest.controller.util.wrapToOk
import cz.havasi.service.SentNotificationService
import cz.havasi.service.UserNotificationService
import cz.havasi.service.UserService
import io.quarkus.security.identity.SecurityIdentity
import jakarta.annotation.security.RolesAllowed
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import org.jboss.resteasy.reactive.RestResponse

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
internal open class UserController(
    private val userService: UserService,
    private val userNotificationService: UserNotificationService,
    private val sentNotificationService: SentNotificationService,
    private val identity: SecurityIdentity,
) {
    @Deprecated("Shouldn't be directly called by the client or another function as user is created while logging if for the first time.")
    open suspend fun createUser(createUserCommand: CreateUserCommand): RestResponse<ResponseId> =
        userService
            .createUser(createUserCommand)
            .let { ResponseId(it).wrapToOk() }

    @GET
    @Path("/me")
    @RolesAllowed("USER")
    open suspend fun getMe(): RestResponse<User> =
        userService
            .getUserById(identity.getAttribute("id"))
            .wrapToOk()

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
        userNotificationService
            .addUserNotification(AddUserNotificationCommand(userId, addNotificationCommand))
            .let { ResponseId(it).wrapToOk() }

    @GET
    @RequireUserMatch
    @Path("/{userId}/notifications")
    open suspend fun getUserNotifications(
        @PathParam("userId") userId: String,
    ): RestResponse<List<Notification>> =
        userNotificationService
            .getUserNotifications(userId)
            .wrapToOk()

    @GET
    @RequireUserMatch
    @Path("/{userId}/notifications/sent")
    open suspend fun getUserSentNotifications(
        @PathParam("userId") userId: String,
        @DefaultValue("0") @QueryParam("offset") offset: Int,
        @DefaultValue("20") @QueryParam("limit") limit: Int,
        @QueryParam("apartmentId") apartmentId: String?,
        @QueryParam("notificationType") notificationType: String?,
    ): RestResponse<List<SentNotification>> =
        sentNotificationService
            .getSentNotifications(
                createGetSentNotificationsCommand(userId, apartmentId, notificationType, offset, limit),
            )
            .wrapToOk()

    @DELETE
    @RequireUserMatch
    @Path("/{userId}/notifications/{notificationId}")
    open suspend fun removeUserNotification(
        @PathParam("userId") userId: String,
        @PathParam("notificationId") notificationId: String,
    ): RestResponse<Nothing> =
        userNotificationService
            .removeUserNotification(notificationId)
            .takeIf { it }
            ?.wrapToNoContent()
            ?: throw ServerErrorException("Notification was not removed.", 500)

    @POST
    @RequireUserMatch
    @Path("/{userId}/notifications/{notificationId}/enable")
    open suspend fun enableUserNotification(
        @PathParam("userId") userId: String,
        @PathParam("notificationId") notificationId: String,
    ): RestResponse<Nothing> =
        userNotificationService
            .enableUserNotification(notificationId)
            .takeIf { it }
            ?.wrapToNoContent()
            ?: throw ServerErrorException("Notification was not enabled.", 500)

    @POST
    @RequireUserMatch
    @Path("/{userId}/notifications/{notificationId}/disable")
    open suspend fun disableUserNotification(
        @PathParam("userId") userId: String,
        @PathParam("notificationId") notificationId: String,
    ): RestResponse<Nothing> =
        userNotificationService
            .disableUserNotification(notificationId)
            .takeIf { it }
            ?.wrapToNoContent()
            ?: throw ServerErrorException("Notification was not disabled.", 500)

    private fun createGetSentNotificationsCommand(
        userId: String,
        apartmentId: String?,
        notificationType: String?,
        offset: Int,
        limit: Int,
    ) = GetSentNotifications(
        userId = userId,
        paging = Paging(
            offset = offset.coerceAtLeast(0),
            limit = limit.coerceIn(10, 20),
            sortBy = "sentAt",
        ),
        apartmentId = apartmentId,
        notificationType = notificationType?.let {
            when (it) {
                "discord" -> NotificationType.DISCORD
                "email" -> NotificationType.EMAIL
                "webhook" -> NotificationType.WEBHOOK
                else -> null
            }
        },
    )
}
