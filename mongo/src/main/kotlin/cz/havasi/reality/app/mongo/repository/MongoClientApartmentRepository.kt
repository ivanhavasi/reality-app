package cz.havasi.reality.app.mongo.repository

import com.mongodb.client.model.Accumulators
import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.Field
import com.mongodb.client.model.Filters
import com.mongodb.client.model.InsertManyOptions
import com.mongodb.client.model.Sorts
import com.mongodb.client.model.UpdateOneModel
import com.mongodb.client.model.Updates
import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.ApartmentDuplicate
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.CurrencyType
import cz.havasi.reality.app.model.Locality
import cz.havasi.reality.app.model.MarketStatistics
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.FindRealEstatesCommand
import cz.havasi.reality.app.model.command.GetStatisticsCommand
import cz.havasi.reality.app.model.command.GroupByDimension
import cz.havasi.reality.app.model.command.TimeGranularity
import cz.havasi.reality.app.model.command.UpdateApartmentWithDuplicateCommand
import cz.havasi.reality.app.model.type.ProviderType
import cz.havasi.reality.app.mongo.DatabaseNames.APARTMENT_COLLECTION_NAME
import cz.havasi.reality.app.mongo.DatabaseNames.DB_NAME
import cz.havasi.reality.app.mongo.entity.ApartmentDuplicateEntity
import cz.havasi.reality.app.mongo.entity.ApartmentEntity
import cz.havasi.reality.app.mongo.entity.LocalityEntity
import cz.havasi.reality.app.mongo.entity.type.ProviderTypeEntity
import cz.havasi.reality.app.mongo.util.toFindOptions
import cz.havasi.reality.app.service.repository.ApartmentRepository
import io.quarkus.logging.Log
import io.quarkus.mongodb.FindOptions
import io.quarkus.mongodb.reactive.ReactiveMongoClient
import io.smallrye.mutiny.coroutines.asFlow
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import java.util.regex.Pattern

@ApplicationScoped
public class MongoClientApartmentRepository(
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
            ?: error("Apartment with id ${apartment.id} was not saved into mongodb.")

    override suspend fun saveAll(apartments: List<Apartment>): List<String> =
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
                ?.map { it.value.asObjectId().value.toHexString() }
                ?: error("No apartments were saved among the ids: ${apartments.map { a -> a.id }}")
        } catch (e: Exception) {
            Log.error("Error while saving apartments", e)
            emptyList()
        }

    override suspend fun bulkUpdateApartmentWithDuplicate(apartmentsWithDuplicates: List<UpdateApartmentWithDuplicateCommand>) {
        val bulkUpdates = apartmentsWithDuplicates.map {
            UpdateOneModel<ApartmentEntity>(
                Filters.eq("externalId", it.apartment.id),
                Updates.combine(
                    Updates.set("locality.latitude", it.apartment.locality.latitude),
                    Updates.set("locality.longitude", it.apartment.locality.longitude),
                    Updates.push("duplicates", it.duplicate.toEntity()),
                    Updates.set("updatedAt", OffsetDateTime.now(UTC)),
                ),
            )
        }
        if (bulkUpdates.isEmpty()) {
            return // No updates to perform
        }

        val result = try {
            mongoCollection
                .bulkWrite(bulkUpdates, BulkWriteOptions().ordered(false)) // all writes should finish even if one fails
                .awaitSuspending()
        } catch (e: Throwable) {
            Log.error("Error while bulkWriting apartments: ${apartmentsWithDuplicates.map { it.apartment.id }}", e)
            return
        }

        if (result.modifiedCount != apartmentsWithDuplicates.size) {
            Log.error(
                "Not all apartments were updated. Expected ${apartmentsWithDuplicates.size} but got" +
                    "${result.modifiedCount}. All apartmentIds: ${apartmentsWithDuplicates.map { it.apartment.id }}",
            )
        }
    }

    override suspend fun findAll(command: FindRealEstatesCommand): List<Apartment> =
        mongoCollection
            .find(createFindQuery(command), command.paging.toFindOptions())
            .asFlow()
            .map { it.toModel() }
            .toList()

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

    override suspend fun findByIdOrFingerprint(id: String, fingerprint: String): List<Apartment> {
        val options = FindOptions().sort(Document("createdAt", -1))
        val filters = mutableListOf<Bson>()
        filters.add(Filters.eq("externalId", id))
        filters.add(Filters.eq("fingerprint", fingerprint))

        return mongoCollection.find(Filters.or(filters), ApartmentEntity::class.java, options)
            .asFlow()
            .map { it.toModel() }
            .toList()
    }

    private fun createFindQuery(command: FindRealEstatesCommand): Bson {
        if (command.searchString.isNullOrBlank()) {
            return command.createBaseFilters()
        }
        val escaped = Regex.escape(command.searchString!!)
        val regexPattern = Pattern.compile(escaped, Pattern.CASE_INSENSITIVE)

        return Filters.and(
            command.createBaseFilters(),
            Filters.or(
                Filters.regex("externalId", regexPattern),
                Filters.regex("fingerprint", regexPattern),
                Filters.regex("url", regexPattern),
                Filters.regex("name", regexPattern),
            ),
        )
    }

    private fun FindRealEstatesCommand.createBaseFilters() =
        Filters.and(
            Filters.eq("transactionType", transactionType.name),
            Filters.eq("mainCategory", buildingType.name),
            Filters.gte("sizeInM2", sizeMin.toDouble()),
            Filters.lte("sizeInM2", sizeMax.toDouble()),
            Filters.gte("price", priceMin.toDouble()),
            Filters.lte("price", priceMax.toDouble()),
        )

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

    override suspend fun getMarketStatistics(command: GetStatisticsCommand): List<MarketStatistics> {
        val pipeline = buildStatisticsPipeline(command)

        return mongoCollection
            .aggregate(pipeline, Document::class.java)
            .asFlow()
            .map { it.toMarketStatistics() }
            .toList()
    }

    private fun buildStatisticsPipeline(command: GetStatisticsCommand): List<Bson> =
        listOf(
            // Step 1: Add computed fields for lowest prices
            Aggregates.addFields(
                Field(
                    "lowestPrice",
                    Document(
                        "\$min",
                        Document(
                            "\$concatArrays",
                            listOf(
                                listOf("\$price"),
                                Document("\$ifNull", listOf("\$duplicates.price", emptyList<Double>())),
                            ),
                        ),
                    ),
                ),
                Field(
                    "lowestPricePerM2",
                    Document(
                        "\$min",
                        Document(
                            "\$concatArrays",
                            listOf(
                                listOf(Document("\$ifNull", listOf("\$pricePerM2", Double.MAX_VALUE))),
                                Document("\$ifNull", listOf("\$duplicates.pricePerM2", emptyList<Double>())),
                            ),
                        ),
                    ),
                ),
            ),

            // Step 2: Filter by criteria
            Aggregates.match(createStatisticsFilters(command)),

            // Step 3: Group by time period and dimensions
            Aggregates.group(
                createGroupId(command),
                Accumulators.avg("avgPrice", "\$lowestPrice"),
                Accumulators.avg("avgPricePerM2", "\$lowestPricePerM2"),
                Accumulators.avg("avgSize", "\$sizeInM2"),
                Accumulators.min("minPrice", "\$lowestPrice"),
                Accumulators.max("maxPrice", "\$lowestPrice"),
                Accumulators.sum("count", 1),
            ),

            // Step 4: Sort by time period
            Aggregates.sort(createSort(command)),
        )

    private fun createStatisticsFilters(command: GetStatisticsCommand): Bson {
        val filters = mutableListOf<Bson>()

        // User-specified filters
        command.city?.let { filters.add(Filters.eq("locality.city", it)) }
        command.district?.let { filters.add(Filters.eq("locality.district", it)) }
        command.buildingType?.let { filters.add(Filters.eq("mainCategory", it.name)) }
        command.subCategory?.let { filters.add(Filters.eq("subCategory", it)) }
        command.transactionType?.let { filters.add(Filters.eq("transactionType", it.name)) }
        command.sizeMin?.let { filters.add(Filters.gte("sizeInM2", it)) }
        command.sizeMax?.let { filters.add(Filters.lte("sizeInM2", it)) }
        command.dateFrom?.let { filters.add(Filters.gte("createdAt", it)) }
        command.dateTo?.let { filters.add(Filters.lte("createdAt", it)) }

        // Data quality filters - exclude invalid/outlier data
        filters.add(Filters.gt("price", 0.0))  // Exclude properties with price <= 0
        filters.add(Filters.gt("sizeInM2", 5.0))  // Exclude properties with size <= 5 m²
        filters.add(Filters.lt("sizeInM2", 10000.0))  // Exclude properties with size >= 10,000 m² (extreme outliers)
        filters.add(Filters.gt("pricePerM2", 0.0))  // Exclude properties with pricePerM2 <= 0

        return if (filters.isEmpty()) Document() else Filters.and(filters)
    }

    private fun createGroupId(command: GetStatisticsCommand): Document {
        val groupId = Document()

        // Group by time period
        when (command.granularity) {
            TimeGranularity.DAILY -> {
                groupId["date"] = Document(
                    "\$dateToString",
                    Document()
                        .append("format", "%Y-%m-%d")
                        .append("date", "\$createdAt"),
                )
            }

            TimeGranularity.WEEKLY -> {
                groupId["week"] = Document("\$week", "\$createdAt")
                groupId["year"] = Document("\$year", "\$createdAt")
            }

            TimeGranularity.MONTHLY -> {
                groupId["month"] = Document("\$month", "\$createdAt")
                groupId["year"] = Document("\$year", "\$createdAt")
            }
        }

        // Add additional grouping dimensions
        command.groupBy.forEach { dimension ->
            when (dimension) {
                GroupByDimension.CITY -> groupId["city"] = "\$locality.city"
                GroupByDimension.DISTRICT -> groupId["district"] = "\$locality.district"
                GroupByDimension.STREET -> groupId["street"] = "\$locality.street"
                GroupByDimension.BUILDING_TYPE -> groupId["buildingType"] = "\$mainCategory"
                GroupByDimension.SUB_CATEGORY -> groupId["subCategory"] = "\$subCategory"
                GroupByDimension.TRANSACTION_TYPE -> groupId["transactionType"] = "\$transactionType"
            }
        }

        return groupId
    }

    private fun createSort(command: GetStatisticsCommand): Bson {
        val sorts = mutableListOf<Bson>()

        when (command.granularity) {
            TimeGranularity.DAILY -> sorts.add(Sorts.ascending("_id.date"))
            TimeGranularity.WEEKLY -> {
                sorts.add(Sorts.ascending("_id.year"))
                sorts.add(Sorts.ascending("_id.week"))
            }

            TimeGranularity.MONTHLY -> {
                sorts.add(Sorts.ascending("_id.year"))
                sorts.add(Sorts.ascending("_id.month"))
            }
        }

        return Sorts.orderBy(sorts)
    }

    private fun Document.toMarketStatistics(): MarketStatistics {
        val id = this.get("_id", Document::class.java)

        // Extract period
        val period = when {
            id.containsKey("date") -> id.getString("date")
            id.containsKey("week") -> "${id.getInteger("year")}-W${id.getInteger("week").toString().padStart(2, '0')}"
            id.containsKey("month") ->
                "${id.getInteger("year")}-${id.getInteger("month").toString().padStart(2, '0')}"

            else -> "unknown"
        }

        // Extract grouping dimensions
        val grouping = mutableMapOf<String, String?>()
        id.getString("city")?.let { grouping["city"] = it }
        id.getString("district")?.let { grouping["district"] = it }
        id.getString("street")?.let { grouping["street"] = it }
        id.getString("buildingType")?.let { grouping["buildingType"] = it }
        id.getString("subCategory")?.let { grouping["subCategory"] = it }
        id.getString("transactionType")?.let { grouping["transactionType"] = it }

        return MarketStatistics(
            period = period,
            grouping = grouping,
            avgPrice = this.getDouble("avgPrice"),
            avgPricePerM2 = this.get("avgPricePerM2") as? Double,
            avgSize = this.getDouble("avgSize"),
            minPrice = this.getDouble("minPrice"),
            maxPrice = this.getDouble("maxPrice"),
            count = this.getInteger("count", 0).toLong(),
        )
    }
}
