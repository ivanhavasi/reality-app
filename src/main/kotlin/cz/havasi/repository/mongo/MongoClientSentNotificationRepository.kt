package cz.havasi.repository.mongo

import cz.havasi.model.*
import cz.havasi.model.command.SaveSentNotificationsCommand
import cz.havasi.repository.DatabaseNames.DB_NAME
import cz.havasi.repository.DatabaseNames.SENT_NOTIFICATION_COLLECTION_NAME
import cz.havasi.repository.SentNotificationRepository
import cz.havasi.repository.entity.SentNotificationEntity
import cz.havasi.repository.entity.SentNotificationRealEstateEntity
import cz.havasi.repository.entity.enum.ProviderTypeEntity
import io.quarkus.mongodb.reactive.ReactiveMongoClient
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import org.bson.types.ObjectId
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC

@ApplicationScoped
internal class MongoClientSentNotificationRepository(
    private val reactiveMongoClient: ReactiveMongoClient,
) : SentNotificationRepository {

    private val mongoCollection = reactiveMongoClient.getDatabase(DB_NAME)
        .getCollection(SENT_NOTIFICATION_COLLECTION_NAME, SentNotificationEntity::class.java)

    override suspend fun saveSentNotifications(saveSentNotificationsCommand: SaveSentNotificationsCommand): Unit {
        mongoCollection
            .insertMany(saveSentNotificationsCommand.toSentNotificationEntity())
            .awaitSuspending()
            .insertedIds
    }

    private fun SaveSentNotificationsCommand.toSentNotificationEntity() = notifications.map {
        SentNotificationEntity(
            _id = ObjectId.get(),
            notificationId = ObjectId(it.id),
            userId = ObjectId(it.userId),
            type = it.toNotificationType(),
            realEstate = apartment.toSentNotificationRealEstateEntity(),
            sentAt = OffsetDateTime.now(UTC),
        )
    }

    private fun Notification.toNotificationType() = when (this) {
        is DiscordWebhookNotification -> "discord"
        is EmailNotification -> "email"
        is WebhookNotification -> "webhook"
    }

    private fun Apartment.toSentNotificationRealEstateEntity() = SentNotificationRealEstateEntity(
        id = id,
        name = name,
        url = url,
        price = price,
        city = locality.city,
        image = images.firstOrNull() ?: "",
        provider = ProviderTypeEntity.valueOf(provider.name),
    )
}