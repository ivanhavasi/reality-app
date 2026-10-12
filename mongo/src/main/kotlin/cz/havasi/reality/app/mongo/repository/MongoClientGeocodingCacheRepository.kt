package cz.havasi.reality.app.mongo.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import cz.havasi.reality.app.model.Coordinates
import cz.havasi.reality.app.model.StreetSegment
import cz.havasi.reality.app.mongo.DatabaseNames.DB_NAME
import cz.havasi.reality.app.mongo.DatabaseNames.GEOCODING_CACHE_COLLECTION_NAME
import cz.havasi.reality.app.mongo.entity.GeocodedStreetEntity
import cz.havasi.reality.app.mongo.entity.StreetSegmentEntity
import cz.havasi.reality.app.service.repository.GeocodingCacheRepository
import io.quarkus.mongodb.reactive.ReactiveMongoClient
import io.smallrye.mutiny.coroutines.asFlow
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.flow.firstOrNull
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC

@ApplicationScoped
internal class MongoClientGeocodingCacheRepository(
    reactiveMongoClient: ReactiveMongoClient,
) : GeocodingCacheRepository {
    private val mongoCollection =
        reactiveMongoClient.getDatabase(DB_NAME).getCollection(GEOCODING_CACHE_COLLECTION_NAME, GeocodedStreetEntity::class.java)

    override suspend fun findStreet(key: String): List<StreetSegment>? =
        mongoCollection.find(Filters.eq("_id", key), GeocodedStreetEntity::class.java)
            .asFlow()
            .firstOrNull()
            ?.segments
            ?.map { StreetSegment(Coordinates(it.latitude, it.longitude), it.description) }

    override suspend fun saveStreet(key: String, segments: List<StreetSegment>) {
        mongoCollection.replaceOne(
            Filters.eq("_id", key),
            GeocodedStreetEntity(
                _id = key,
                segments = segments.map { StreetSegmentEntity(it.coordinates.latitude, it.coordinates.longitude, it.description) },
                createdAt = OffsetDateTime.now(UTC),
            ),
            ReplaceOptions().upsert(true),
        ).awaitSuspending()
    }
}
