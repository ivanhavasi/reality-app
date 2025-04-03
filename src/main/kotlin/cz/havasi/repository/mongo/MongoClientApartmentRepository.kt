package cz.havasi.repository.mongo

import com.mongodb.client.model.*
import cz.havasi.model.*
import cz.havasi.model.command.UpdateApartmentWithDuplicateCommand
import cz.havasi.model.enum.ProviderType
import cz.havasi.repository.ApartmentRepository
import cz.havasi.repository.DatabaseNames.APARTMENT_COLLECTION_NAME
import cz.havasi.repository.DatabaseNames.DB_NAME
import cz.havasi.repository.entity.ApartmentDuplicateEntity
import cz.havasi.repository.entity.ApartmentEntity
import cz.havasi.repository.entity.LocalityEntity
import cz.havasi.repository.entity.enum.ProviderTypeEntity
import io.quarkus.logging.Log
import io.quarkus.mongodb.reactive.ReactiveMongoClient
import io.smallrye.mutiny.coroutines.asFlow
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.flow.firstOrNull
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC

@ApplicationScoped
internal class MongoClientApartmentRepository(
    private val reactiveMongoClient: ReactiveMongoClient,
) : ApartmentRepository {

    private val mongoCollection =
        reactiveMongoClient.getDatabase(DB_NAME).getCollection(APARTMENT_COLLECTION_NAME, ApartmentEntity::class.java)

    override suspend fun save(apartment: Apartment): String =
        mongoCollection
            .insertOne(apartment.toEntity())
            ?.awaitSuspending()
            ?.insertedId
            ?.asObjectId()
            ?.value
            ?.toHexString()
            ?: throw error("Apartment with id ${apartment.id} was not saved into mongodb.")

    override suspend fun saveAll(apartments: List<Apartment>): List<ObjectId> =
        try {
            mongoCollection
                .insertMany(
                    apartments.map {
                        it.toEntity()
                    },
                    InsertManyOptions().ordered(false),
                )
                ?.awaitSuspending()
                ?.insertedIds
                ?.map { it.value.asObjectId().value }
                ?: throw error("No apartments were saved among the ids: ${apartments.map { a -> a.id }}")
        } catch (e: Exception) {
            Log.error("Error while saving apartments", e)
            emptyList()
        }

    override suspend fun bulkUpdateApartmentWithDuplicate(apartmentsWithDuplicates: List<UpdateApartmentWithDuplicateCommand>) {
        val bulkUpdates = apartmentsWithDuplicates.map {
            UpdateOneModel<ApartmentEntity>(
                Filters.or(
                    Filters.eq("externalId", it.apartment.id),
                    Filters.eq("fingerprint", it.apartment.fingerprint),
                ),
                Updates.combine(
                    Updates.push("duplicates", it.duplicate.toEntity()),
                    Updates.set("updatedAt", OffsetDateTime.now(UTC).toString()),
                ),
            )
        }
        if (bulkUpdates.isEmpty()) {
            Log.warn("No updates to perform for apartments: ${apartmentsWithDuplicates.map { it.apartment.id }}")
            return // No updates to perform
        }

        val result = try {
            mongoCollection.bulkWrite(bulkUpdates, BulkWriteOptions().ordered(false))
                .awaitSuspending()
        } catch (e: Throwable) {
            Log.error("Error whilebulkWrite apartments: ${apartmentsWithDuplicates.map { it.apartment.id }}", e)
            return
        }

        Log.debug("Bulk write update count: ${result.modifiedCount} out of ${apartmentsWithDuplicates.size}")
        if (result.modifiedCount != apartmentsWithDuplicates.size) {
            Log.error(result)
            Log.error(
                "Not all apartments were updated. Expected ${apartmentsWithDuplicates.size} but got" +
                    "${result.modifiedCount}. All apartmentIds: ${apartmentsWithDuplicates.map { it.apartment.id }}",
            )
        }
    }

    override suspend fun existsByIdOrFingerprint(id: String, fingerprint: String): Boolean {
        val filters = mutableListOf<Bson>()
        filters.add(Filters.eq("externalId", id))
        filters.add(Filters.eq("fingerprint", fingerprint))

        return mongoCollection
            .find(Filters.or(filters), ApartmentEntity::class.java)
            .asFlow()
            .firstOrNull()
            .let { it != null }
    }

    override suspend fun findByIdOrFingerprint(id: String, fingerprint: String): Apartment? {
        val filters = mutableListOf<Bson>()
        filters.add(Filters.eq("externalId", id))
        filters.add(Filters.eq("fingerprint", fingerprint))

        return mongoCollection.find(Filters.or(filters), ApartmentEntity::class.java)
            .asFlow()
            .firstOrNull()
            ?.toModel()
    }

    private fun ApartmentEntity.toModel() =
        Apartment(
            id = externalId,
            fingerprint = fingerprint,
            locality = locality.toModel(),
            price = price,
            name = name,
            url = url,
            pricePerM2 = pricePerM2,
            sizeInM2 = sizeInM2,
            currency = CurrencyType.valueOf(currency),
            mainCategory = BuildingType.valueOf(mainCategory),
            subCategory = subCategory,
            transactionType = TransactionType.valueOf(transactionType),
            images = images,
            description = description,
            provider = ProviderType.valueOf(provider.name),
            duplicates = duplicates.map { it.toModel() },
        )

    private fun ApartmentDuplicateEntity.toModel() =
        ApartmentDuplicate(
            url = url,
            price = price,
            pricePerM2 = pricePerM2,
            images = images,
            provider = ProviderType.valueOf(provider.name),
        )

    private fun LocalityEntity.toModel() =
        Locality(
            city = city,
            district = district,
            street = street,
            streetNumber = streetNumber,
            latitude = latitude,
            longitude = longitude,
        )

    private fun Apartment.toEntity() =
        ApartmentEntity(
            _id = ObjectId.get(),
            externalId = id,
            fingerprint = fingerprint,
            name = name,
            url = url,
            price = price,
            pricePerM2 = pricePerM2,
            sizeInM2 = sizeInM2,
            currency = currency.name,
            locality = locality.toEntity(),
            mainCategory = mainCategory.name,
            subCategory = subCategory,
            transactionType = transactionType.name,
            images = images,
            description = description,
            provider = ProviderTypeEntity.valueOf(provider.name),
            duplicates = duplicates.map { it.toEntity() },
            createdAt = OffsetDateTime.now(UTC),
            updatedAt = OffsetDateTime.now(UTC),
        )

    private fun ApartmentDuplicate.toEntity() =
        ApartmentDuplicateEntity(
            url = url,
            price = price,
            pricePerM2 = pricePerM2,
            images = images,
            provider = ProviderTypeEntity.valueOf(provider.name),
        )

    private fun Locality.toEntity() =
        LocalityEntity(
            city = city,
            district = district,
            street = street,
            streetNumber = streetNumber,
            latitude = latitude,
            longitude = longitude,
        )
}
