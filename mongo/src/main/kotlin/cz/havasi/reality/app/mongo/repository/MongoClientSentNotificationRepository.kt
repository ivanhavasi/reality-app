package cz.havasi.reality.app.mongo.repository

import com.mongodb.client.model.Filters
import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.SentNotification
import cz.havasi.reality.app.model.SentNotificationRealEstate
import cz.havasi.reality.app.model.command.GetSentNotifications
import cz.havasi.reality.app.model.command.SaveSentNotificationsCommand
import cz.havasi.reality.app.model.type.NotificationType
import cz.havasi.reality.app.model.type.ProviderType
import cz.havasi.reality.app.mongo.DatabaseNames.DB_NAME
import cz.havasi.reality.app.mongo.DatabaseNames.SENT_NOTIFICATION_COLLECTION_NAME
import cz.havasi.reality.app.mongo.entity.SentNotificationEntity
import cz.havasi.reality.app.mongo.entity.SentNotificationRealEstateEntity
import cz.havasi.reality.app.mongo.entity.type.NotificationTypeEntity
import cz.havasi.reality.app.mongo.entity.type.ProviderTypeEntity
import cz.havasi.reality.app.mongo.util.toFindOptions
import cz.havasi.reality.app.service.repository.SentNotificationRepository
import io.quarkus.mongodb.reactive.ReactiveMongoClient
import io.smallrye.mutiny.coroutines.asFlow
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.flow.toList
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC

@ApplicationScoped
internal class MongoClientSentNotificationRepository(
    private val reactiveMongoClient: ReactiveMongoClient,
) : SentNotificationRepository {
    private val mongoCollection = reactiveMongoClient.getDatabase(DB_NAME)
        .getCollection(SENT_NOTIFICATION_COLLECTION_NAME, SentNotificationEntity::class.java)

    override suspend fun saveSentNotifications(command: SaveSentNotificationsCommand): Unit {
        mongoCollection
            .insertMany(command.toSentNotificationEntity())
            .awaitSuspending()
            .insertedIds
    }

    override suspend fun getSentNotifications(command: GetSentNotifications): List<SentNotification> =
        mongoCollection
            .find(command.createGetSentNotificationFilters(), command.paging.toFindOptions())
            .map { it.toModel() }
            .asFlow()
            .toList()

    private fun GetSentNotifications.createGetSentNotificationFilters(): Bson {
        val filters = mutableListOf<Bson>()
        filters.add(Filters.eq("userId", ObjectId(userId)))
        if (apartmentId != null) {
            filters.add(Filters.eq("realEstate.id", apartmentId))
        }
        if (notificationType != null) {
            // smart cast not working, because 'notificationType' is a public API property declared in different module
            filters.add(Filters.eq("type", NotificationTypeEntity.valueOf(notificationType!!.name)))
        }

        return Filters.and(filters)
    }

    private fun SaveSentNotificationsCommand.toSentNotificationEntity() = notifications.map {
        SentNotificationEntity(
            _id = ObjectId.get(),
            notificationId = ObjectId(it.id),
            userId = ObjectId(it.userId),
            type = NotificationTypeEntity.valueOf(it.type.name),
            realEstate = apartment.toSentNotificationRealEstateEntity(),
            sentAt = OffsetDateTime.now(UTC),
        )
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

    private fun SentNotificationEntity.toModel() = SentNotification(
        notificationId = notificationId.toHexString(),
        userId = userId.toHexString(),
        type = NotificationType.valueOf(type.name),
        realEstate = realEstate.toModel(),
        sentAt = sentAt,
    )

    private fun SentNotificationRealEstateEntity.toModel() = SentNotificationRealEstate(
        id = id,
        name = name,
        url = url,
        price = price,
        city = city,
        image = image,
        provider = ProviderType.valueOf(provider.name),
    )
}
