package cz.havasi.repository.mongo

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import cz.havasi.model.DiscordWebhookNotification
import cz.havasi.model.EmailNotification
import cz.havasi.model.Notification
import cz.havasi.model.WebhookNotification
import cz.havasi.model.command.*
import cz.havasi.repository.DatabaseNames.DB_NAME
import cz.havasi.repository.DatabaseNames.USER_NOTIFICATION_COLLECTION_NAME
import cz.havasi.repository.UserNotificationRepository
import cz.havasi.repository.entity.DiscordWebhookNotificationEntity
import cz.havasi.repository.entity.EmailNotificationEntity
import cz.havasi.repository.entity.NotificationEntity
import cz.havasi.repository.entity.WebhookNotificationEntity
import io.quarkus.mongodb.reactive.ReactiveMongoClient
import io.smallrye.mutiny.coroutines.asFlow
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC

@ApplicationScoped
internal class MongoClientUserNotificationRepository(
    private val reactiveMongoClient: ReactiveMongoClient,
) : UserNotificationRepository {
    private val mongoCollection = reactiveMongoClient.getDatabase(DB_NAME)
        .getCollection(USER_NOTIFICATION_COLLECTION_NAME, NotificationEntity::class.java)

    override suspend fun addUserNotification(command: AddUserNotificationCommand): String =
        mongoCollection.insertOne(command.toEntity())
            .awaitSuspending()
            .insertedId
            ?.asObjectId()
            ?.value
            ?.toHexString()
            ?: throw error("Notification for user with id ${command.userId} was not saved into mongo db")

    override suspend fun removeUserNotification(notificationId: String): Boolean =
        mongoCollection.deleteOne(
            Filters.eq("_id", ObjectId(notificationId)),
        )
            .awaitSuspending()
            .deletedCount > 0

    override suspend fun updateUserNotification(command: UpdateUserNotificationCommand): Boolean =
        mongoCollection.updateOne(
            Filters.eq("_id", ObjectId(command.notificationId)),
            command.toMongoUpdate(),
        )
            .awaitSuspending()
            .modifiedCount > 0

    private fun UpdateUserNotificationCommand.toMongoUpdate() =
        listOfNotNull(
            enabled?.let { Updates.set("enabled", it) },
        )
            .let { Updates.combine(it) }

    override suspend fun getUserNotifications(userId: String): List<Notification> =
        mongoCollection.find(Filters.eq("userId", ObjectId(userId)), NotificationEntity::class.java)
            .asFlow()
            .toList()
            .map { it.toModel() }

    override suspend fun getUserNotificationById(notificationId: String): Notification =
        mongoCollection.find(Filters.eq("_id", ObjectId(notificationId)), NotificationEntity::class.java)
            .asFlow()
            .firstOrNull()
            ?.toModel()
            ?: throw error("Notification with id $notificationId was not found in mongo db")

    override suspend fun findUserNotificationsForFilter(command: FindUserNotificationsForFilterCommand): List<Notification> =
        mongoCollection.find(command.toMongoFilters(), NotificationEntity::class.java)
            .asFlow()
            .toList()
            .map { it.toModel() }

    private fun FindUserNotificationsForFilterCommand.toMongoFilters(): Bson {
        val filters = mutableListOf<Bson>()
        filters.add(Filters.eq("filter.buildingType", buildingType))
        filters.add(Filters.eq("filter.transactionType", transactionType))
        filters.add(Filters.eq("enabled", true))
        size.addRangeFilter(filters, "size")
        price.addRangeFilter(filters, "price")
        filters.add(
            Filters.or(
                Filters.not(Filters.exists("filter.subTypes")),
                Filters.`in`("filter.subTypes", subTypes),
            ),
        )
        return Filters.and(filters)
    }

    /**
     * Adds a range filter to the list of BSON filters.
     * The filters are applied to fields named `filter.<name>.from` and `filter.<name>.to`. Or when the fields do not
     * exist.
     *
     * @param T the type of the number, a subclass of `Number`
     * @param filterList the list of BSON filters
     * @param name the filter field name, only 'size' and 'price' are supported
     */
    private fun <T : Number> T.addRangeFilter(filterList: MutableList<Bson>, name: String) {
        let {
            filterList.add(
                Filters.and(
                    Filters.or(
                        Filters.lte("filter.$name.from", it),
                        Filters.exists("filter.$name.from", false),
                    ),
                    Filters.or(
                        Filters.gte("filter.$name.to", it),
                        Filters.exists("filter.$name.to", false),
                    ),
                ),
            )
        }
    }

    private fun AddUserNotificationCommand.toEntity() =
        when (notification) {
            is EmailNotificationCommand -> EmailNotificationEntity(
                id = ObjectId.get(),
                name = notification.name,
                filter = notification.filter,
                updatedAt = OffsetDateTime.now(UTC),
                createdAt = OffsetDateTime.now(UTC),
                email = notification.email,
                userId = ObjectId(userId),
                enabled = true,
            )

            is WebhookNotificationCommand -> WebhookNotificationEntity(
                id = ObjectId.get(),
                name = notification.name,
                filter = notification.filter,
                updatedAt = OffsetDateTime.now(UTC),
                createdAt = OffsetDateTime.now(UTC),
                url = notification.url,
                userId = ObjectId(userId),
                enabled = true,
            )

            is DiscordWebhookNotificationCommand -> DiscordWebhookNotificationEntity(
                id = ObjectId.get(),
                name = notification.name,
                filter = notification.filter,
                updatedAt = OffsetDateTime.now(UTC),
                createdAt = OffsetDateTime.now(UTC),
                webhookId = notification.webhookId,
                token = notification.token,
                userId = ObjectId(userId),
                enabled = true,
            )
        }

    private fun NotificationEntity.toModel() =
        when (this) {
            is EmailNotificationEntity -> EmailNotification(
                id = id.toHexString(),
                name = name,
                userId = userId.toHexString(),
                filter = filter,
                updatedAt = updatedAt,
                createdAt = createdAt,
                email = email,
                enabled = enabled,
            )

            is WebhookNotificationEntity -> WebhookNotification(
                id = id.toHexString(),
                name = name,
                userId = userId.toHexString(),
                filter = filter,
                updatedAt = updatedAt,
                createdAt = createdAt,
                url = url,
                enabled = enabled,
            )

            is DiscordWebhookNotificationEntity -> DiscordWebhookNotification(
                id = id.toHexString(),
                name = name,
                userId = userId.toHexString(),
                filter = filter,
                updatedAt = updatedAt,
                createdAt = createdAt,
                webhookId = webhookId,
                token = token,
                enabled = enabled,
            )
        }
}
