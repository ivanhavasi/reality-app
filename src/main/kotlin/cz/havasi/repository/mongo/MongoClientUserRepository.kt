package cz.havasi.repository.mongo

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import cz.havasi.model.Notification.EmailNotification
import cz.havasi.model.Notification.WebhookNotification
import cz.havasi.model.User
import cz.havasi.model.command.AddNotificationCommand
import cz.havasi.model.command.AddUserNotificationCommand
import cz.havasi.model.command.CreateUserCommand
import cz.havasi.model.command.EmailNotificationCommand
import cz.havasi.model.command.RemoveUserNotificationCommand
import cz.havasi.model.command.WebhookNotificationCommand
import cz.havasi.repository.DatabaseNames.DB_NAME
import cz.havasi.repository.DatabaseNames.USER_COLLECTION_NAME
import cz.havasi.repository.UserRepository
import cz.havasi.repository.entity.EmailNotificationEntity
import cz.havasi.repository.entity.NotificationEntity
import cz.havasi.repository.entity.UserEntity
import cz.havasi.repository.entity.WebhookNotificationEntity
import io.quarkus.mongodb.reactive.ReactiveMongoClient
import io.smallrye.mutiny.coroutines.asFlow
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.flow.firstOrNull
import org.bson.types.ObjectId
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC

@ApplicationScoped
internal class MongoClientUserRepository(
    private val reactiveMongoClient: ReactiveMongoClient,
) : UserRepository {

    private val mongoCollection =
        reactiveMongoClient.getDatabase(DB_NAME).getCollection(USER_COLLECTION_NAME, UserEntity::class.java)

    override suspend fun save(command: CreateUserCommand): String =
        mongoCollection.insertOne(command.toEntity())
            ?.awaitSuspending()
            ?.insertedId
            ?.asObjectId()
            ?.value
            ?.toHexString()
            ?: throw error("User with email ${command.email} was not saved into mongo db")

    override suspend fun getUserById(id: String): User =
        mongoCollection.find(Filters.eq("_id", ObjectId(id)), UserEntity::class.java)
            .asFlow()
            .firstOrNull()
            ?.toModel()
            ?: throw error("User with id $id was not found in mongo db")

    override suspend fun addUserNotification(command: AddUserNotificationCommand): Boolean =
        mongoCollection.updateOne(
            Filters.eq("_id", ObjectId(command.userId)),
            Updates.addToSet("notifications", command.notification.toEntity()),
        )
            .awaitSuspending()
            .modifiedCount > 0

    override suspend fun removeUserNotification(command: RemoveUserNotificationCommand): Boolean =
        mongoCollection.updateOne(
            Filters.eq("_id", ObjectId(command.userId)),
            Updates.pull(UserEntity::notifications.name, Filters.eq("_id", ObjectId(command.notificationId)))
        )
            .awaitSuspending()
            .modifiedCount > 0

    private fun UserEntity.toModel() =
        User(
            id = _id.toHexString(),
            email = email,
            username = username,
            notifications = notifications.map { it.toModel() },
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun NotificationEntity.toModel() =
        when (this) {
            is EmailNotificationEntity -> EmailNotification(
                id = id.toHexString(),
                name = name,
                filter = filter,
                updatedAt = updatedAt,
                createdAt = createdAt,
                email = email,
            )

            is WebhookNotificationEntity -> WebhookNotification(
                id = id.toHexString(),
                name = name,
                filter = filter,
                updatedAt = updatedAt,
                createdAt = createdAt,
                url = url,
            )
        }

    private fun AddNotificationCommand.toEntity() =
        when (this) {
            is EmailNotificationCommand -> EmailNotificationEntity(
                id = ObjectId.get(),
                name = name,
                filter = filter,
                updatedAt = OffsetDateTime.now(UTC),
                createdAt = OffsetDateTime.now(UTC),
                email = email,
            )

            is WebhookNotificationCommand -> WebhookNotificationEntity(
                id = ObjectId.get(),
                name = name,
                filter = filter,
                updatedAt = OffsetDateTime.now(UTC),
                createdAt = OffsetDateTime.now(UTC),
                url = url,
            )
        }

    private fun CreateUserCommand.toEntity() =
        UserEntity(
            _id = ObjectId.get(),
            email = email,
            username = username,
            notifications = emptyList(),
            createdAt = OffsetDateTime.now(UTC),
            updatedAt = OffsetDateTime.now(UTC),
        )
}
