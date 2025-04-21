package cz.havasi.rest.controller

import cz.havasi.config.security.RequireUserMatch
import cz.havasi.model.Notification
import cz.havasi.model.SentNotification
import cz.havasi.model.User
import cz.havasi.model.command.*
import cz.havasi.model.enum.NotificationType
import cz.havasi.model.util.Paging
import cz.havasi.rest.controller.model.ResponseId
import cz.havasi.rest.controller.util.wrapToNoContent
import cz.havasi.rest.controller.util.wrapToOk
import cz.havasi.service.NotificationService
import cz.havasi.service.SentNotificationService
import cz.havasi.service.UserService
import io.quarkus.security.identity.SecurityIdentity
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import org.jboss.resteasy.reactive.RestResponse

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
internal open class UserController(
    private val userService: UserService,
    private val notificationService: NotificationService,
    private val sentNotificationService: SentNotificationService,
    private val identity: SecurityIdentity,
) {
    // shouldn't be called
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

    @GET
    @RequireUserMatch
    @Path("/{userId}/notifications/sent")
    open suspend fun getUserSentNotifications(
        @PathParam("userId") userId: String,
        @QueryParam("offset") offset: Int = 0,
        @QueryParam("limit") limit: Int = 20,
        @QueryParam("apartmentId") apartmentId: String? = null,
        @QueryParam("notificationType") notificationType: String? = null,
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
        notificationService
            .removeUserNotification(RemoveUserNotificationCommand(userId, notificationId))
            .takeIf { it }
            ?.wrapToNoContent()
            ?: throw ServerErrorException("Notification was not removed.", 500)

    private fun createGetSentNotificationsCommand(
        userId: String,
        apartmentId: String?,
        notificationType: String?,
        offset: Int,
        limit: Int,
    ) = GetSentNotifications(
        userId = userId,
        paging = Paging(
            offset = offset.coerceIn(0, 20),
            limit = limit.coerceIn(0, 20),
            sortBy = "sentAt",
        ),
        apartmentId = apartmentId,
        notificationType = notificationType?.let {
            try {
                NotificationType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null // todo throw exception for invalid type
            }
        },
    )
}
