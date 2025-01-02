package cz.havasi.repository.mongo

import com.mongodb.client.model.Filters
import cz.havasi.model.Apartment
import cz.havasi.model.Locality
import cz.havasi.repository.ApartmentRepository
import cz.havasi.repository.DatabaseNames.APARTMENT_COLLECTION_NAME
import cz.havasi.repository.DatabaseNames.DB_NAME
import cz.havasi.repository.entity.ApartmentEntity
import cz.havasi.repository.entity.LocalityEntity
import io.quarkus.mongodb.reactive.ReactiveMongoClient
import io.smallrye.mutiny.coroutines.asFlow
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.flow.firstOrNull
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import java.time.OffsetDateTime

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
        mongoCollection
            .insertMany(apartments.map {
                it.toEntity()
            })
            ?.awaitSuspending()
            ?.insertedIds
            ?.map { it.value.asObjectId().value }
            ?: throw error("No apartments were saved among the ids: ${apartments.map { a -> a.id }}")

    override suspend fun existsByIdOrFingerprint(id: String, fingerprint: String): Boolean {
        val filters = mutableListOf<Bson>()
        filters.add(Filters.eq("externalId", id))
        filters.add(Filters.eq("fingerprint", id))

        return mongoCollection
            .find(Filters.or(filters), ApartmentEntity::class.java)
            .asFlow()
            .firstOrNull()
            .let { it != null }
    }

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
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now(),
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
