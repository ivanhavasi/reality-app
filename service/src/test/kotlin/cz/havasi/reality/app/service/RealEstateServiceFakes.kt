package cz.havasi.reality.app.service

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.CurrencyType
import cz.havasi.reality.app.model.DiscordWebhookNotification
import cz.havasi.reality.app.model.EmailNotification
import cz.havasi.reality.app.model.Locality
import cz.havasi.reality.app.model.MarketStatistics
import cz.havasi.reality.app.model.Notification
import cz.havasi.reality.app.model.NotificationFilter
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.WebhookNotification
import cz.havasi.reality.app.model.command.AddUserNotificationCommand
import cz.havasi.reality.app.model.command.FindRealEstatesCommand
import cz.havasi.reality.app.model.command.FindUserNotificationsForFilterCommand
import cz.havasi.reality.app.model.command.GetRealEstatesCommand
import cz.havasi.reality.app.model.command.GetStatisticsCommand
import cz.havasi.reality.app.model.command.UpdateApartmentWithDuplicateCommand
import cz.havasi.reality.app.model.command.UpdateUserNotificationCommand
import cz.havasi.reality.app.model.event.HandleNotificationsEvent
import cz.havasi.reality.app.model.type.ProviderType
import cz.havasi.reality.app.service.provider.RealEstatesProvider
import cz.havasi.reality.app.service.repository.ApartmentRepository
import cz.havasi.reality.app.service.repository.UserNotificationRepository
import cz.havasi.reality.app.service.util.constructFingerprint
import jakarta.enterprise.event.Event
import jakarta.enterprise.event.NotificationOptions
import jakarta.enterprise.util.TypeLiteral
import kotlinx.coroutines.delay
import java.time.OffsetDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

internal fun land(
    id: String,
    provider: ProviderType,
    size: Double,
    price: Double,
    district: String? = null,
    street: String? = null,
): Apartment {
    val locality = Locality(
        city = "Praha",
        district = district,
        street = street,
        streetNumber = null,
        latitude = null,
        longitude = null,
    )
    return Apartment(
        id = id,
        fingerprint = constructFingerprint(BuildingType.LAND, locality, "BUILDING_PLOT", TransactionType.SALE, size),
        name = "Prodej stavebního pozemku $size m²",
        url = "https://example.com/$id",
        price = price,
        pricePerM2 = price / size,
        sizeInM2 = size,
        currency = CurrencyType.CZK,
        locality = locality,
        mainCategory = BuildingType.LAND,
        subCategory = "BUILDING_PLOT",
        transactionType = TransactionType.SALE,
        provider = provider,
    )
}

internal fun apartment(
    id: String,
    provider: ProviderType,
    size: Double,
    price: Double,
    street: String? = "Havlíčkova",
    subCategory: String = "2+kk",
): Apartment {
    val locality = Locality(
        city = "Praha",
        district = "Žižkov",
        street = street,
        streetNumber = null,
        latitude = null,
        longitude = null,
    )
    return Apartment(
        id = id,
        fingerprint = constructFingerprint(BuildingType.APARTMENT, locality, subCategory, TransactionType.SALE),
        name = "Prodej bytu $subCategory $size m²",
        url = "https://example.com/$id",
        price = price,
        pricePerM2 = price / size,
        sizeInM2 = size,
        currency = CurrencyType.CZK,
        locality = locality,
        mainCategory = BuildingType.APARTMENT,
        subCategory = subCategory,
        transactionType = TransactionType.SALE,
        provider = provider,
    )
}

// mirrors the Mongo implementation: id OR fingerprint OR duplicates.id, newest first
internal class FakeApartmentRepository : ApartmentRepository {
    val stored = mutableListOf<Apartment>()

    fun byId(id: String): Apartment? = stored.firstOrNull { it.id == id }

    override suspend fun save(apartment: Apartment): String {
        stored += apartment
        return apartment.id
    }

    override suspend fun saveAll(apartments: List<Apartment>): List<String> = apartments.map { save(it) }

    override suspend fun bulkUpdateApartmentWithDuplicate(apartments: List<UpdateApartmentWithDuplicateCommand>) {
        apartments.forEach { command ->
            val index = stored.indexOfFirst { it.id == command.apartment.id }
            val current = stored[index]
            stored[index] = current.copy(
                locality = current.locality.copy(
                    latitude = command.apartment.locality.latitude,
                    longitude = command.apartment.locality.longitude,
                ),
                duplicates = current.duplicates + command.duplicate,
            )
        }
    }

    override suspend fun findAll(command: FindRealEstatesCommand): List<Apartment> = stored.toList()

    override suspend fun existsByIdOrFingerprint(id: String, fingerprint: String): Boolean =
        findByIdOrFingerprint(id, fingerprint).isNotEmpty()

    override suspend fun findByIdOrFingerprint(id: String, fingerprint: String): List<Apartment> =
        stored
            .filter { it.id == id || it.fingerprint == fingerprint || it.duplicates.any { d -> d.id == id } }
            .reversed()

    override suspend fun getMarketStatistics(command: GetStatisticsCommand): List<MarketStatistics> = emptyList()
}

internal class FakeProvider(
    private val name: String,
    private val events: MutableList<String> = mutableListOf(),
    private val callDelayMillis: Long = 0,
    private val pages: (page: Int) -> List<Apartment>,
) : RealEstatesProvider {
    val commands = mutableListOf<GetRealEstatesCommand>()

    override suspend fun getRealEstates(getRealEstatesCommand: GetRealEstatesCommand): List<Apartment> {
        commands += getRealEstatesCommand
        events += "$name-start"
        delay(callDelayMillis)
        events += "$name-end"
        return pages(getRealEstatesCommand.offset / getRealEstatesCommand.limit)
    }

    override fun toString(): String = name
}

internal class RecordingEvent<T> : Event<T> {
    val fired = mutableListOf<T>()

    override fun fire(event: T) {
        fired += event
    }

    override fun <U : T> fireAsync(event: U): CompletionStage<U> = CompletableFuture.completedFuture(event)

    override fun <U : T> fireAsync(event: U, options: NotificationOptions): CompletionStage<U> =
        CompletableFuture.completedFuture(event)

    override fun select(vararg qualifiers: Annotation): Event<T> = this

    override fun <U : T> select(subtype: Class<U>, vararg qualifiers: Annotation): Event<U> =
        throw UnsupportedOperationException()

    override fun <U : T> select(subtype: TypeLiteral<U>, vararg qualifiers: Annotation): Event<U> =
        throw UnsupportedOperationException()
}

// every listing matches exactly one email notification, so fired events = notified listings
internal class MatchEverythingNotificationRepository : UserNotificationRepository {
    private val notification = EmailNotification(
        id = "n1",
        name = "everything",
        filter = NotificationFilter(BuildingType.LAND, TransactionType.SALE, null, null, null),
        userId = "u1",
        updatedAt = OffsetDateTime.MIN,
        createdAt = OffsetDateTime.MIN,
        email = "test@example.com",
        enabled = true,
    )

    override suspend fun addUserNotification(command: AddUserNotificationCommand): String = error("unused")
    override suspend fun removeUserNotification(userId: String, notificationId: String): Boolean = error("unused")
    override suspend fun updateUserNotification(command: UpdateUserNotificationCommand): Boolean = error("unused")
    override suspend fun getUserNotifications(userId: String): List<Notification> = error("unused")
    override suspend fun getUserNotificationById(notificationId: String): Notification = error("unused")
    override suspend fun findUserNotificationsForFilter(command: FindUserNotificationsForFilterCommand): List<Notification> =
        listOf(notification)
}

internal class RealEstateServiceFixture(providers: List<RealEstatesProvider>) {
    val repository = FakeApartmentRepository()
    private val emailEvents = RecordingEvent<HandleNotificationsEvent<EmailNotification>>()
    val service = RealEstateService(
        apartmentRepository = repository,
        userNotificationService = UserNotificationService(
            MatchEverythingNotificationRepository(),
            emailEvents,
            RecordingEvent<HandleNotificationsEvent<WebhookNotification>>(),
            RecordingEvent<HandleNotificationsEvent<DiscordWebhookNotification>>(),
        ),
        realEstateProviders = providers.toMutableList(),
    )

    val notifiedIds: List<String> get() = emailEvents.fired.map { it.apartment.id }
}
