package cz.havasi.repository.mongo

import com.mongodb.client.model.Filters
import cz.havasi.model.Apartment
import cz.havasi.model.SentNotification
import cz.havasi.model.SentNotificationRealEstate
import cz.havasi.model.command.GetSentNotifications
import cz.havasi.model.command.SaveSentNotificationsCommand
import cz.havasi.model.enum.NotificationType
import cz.havasi.model.enum.ProviderType
import cz.havasi.repository.DatabaseNames.DB_NAME
import cz.havasi.repository.DatabaseNames.SENT_NOTIFICATION_COLLECTION_NAME
import cz.havasi.repository.SentNotificationRepository
import cz.havasi.repository.entity.SentNotificationEntity
import cz.havasi.repository.entity.SentNotificationRealEstateEntity
import cz.havasi.repository.entity.enum.NotificationTypeEntity
import cz.havasi.repository.entity.enum.ProviderTypeEntity
import cz.havasi.repository.util.toFindOptions
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
            filters.add(Filters.eq("type", NotificationTypeEntity.valueOf(notificationType.name)))
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
