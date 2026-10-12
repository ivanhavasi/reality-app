package cz.havasi.reality.app.mongo.repository

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.Filters
import com.mongodb.client.model.GeoNearOptions.geoNearOptions
import com.mongodb.client.model.ReplaceOneModel
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.geojson.Point
import com.mongodb.client.model.geojson.Position
import cz.havasi.reality.app.model.Coordinates
import cz.havasi.reality.app.model.NearbyPlace
import cz.havasi.reality.app.model.Place
import cz.havasi.reality.app.model.PlaceCategory
import cz.havasi.reality.app.mongo.DatabaseNames.DB_NAME
import cz.havasi.reality.app.mongo.DatabaseNames.PLACE_COLLECTION_NAME
import cz.havasi.reality.app.service.repository.PlaceRepository
import io.quarkus.mongodb.reactive.ReactiveMongoClient
import io.smallrye.mutiny.coroutines.asFlow
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document
import java.util.Date
import kotlin.math.roundToInt

// documents rather than entities: the GeoJSON fields are built by hand
@ApplicationScoped
internal class MongoClientPlaceRepository(
    reactiveMongoClient: ReactiveMongoClient,
) : PlaceRepository {
    private val mongoCollection = reactiveMongoClient.getDatabase(DB_NAME).getCollection(PLACE_COLLECTION_NAME)

    // upserted first, then whatever this run didn't touch is removed: a failure halfway leaves the old places in
    override suspend fun replaceCategory(category: PlaceCategory, places: List<Place>) {
        val importedAt = Date()
        places.chunked(BATCH_SIZE).forEach { batch ->
            mongoCollection.bulkWrite(
                batch.map { place ->
                    ReplaceOneModel(Filters.eq("_id", place.id), place.toDocument(importedAt), ReplaceOptions().upsert(true))
                },
                BulkWriteOptions().ordered(false),
            ).awaitSuspending()
        }
        mongoCollection.deleteMany(Filters.and(Filters.eq("category", category.name), Filters.lt("importedAt", importedAt)))
            .awaitSuspending()
    }

    override suspend fun findNearest(category: PlaceCategory, coordinates: Coordinates, maxDistanceInMeters: Int): NearbyPlace? =
        mongoCollection
            .aggregate(
                listOf(
                    Aggregates.geoNear(
                        Point(Position(coordinates.longitude, coordinates.latitude)),
                        "distance",
                        geoNearOptions()
                            .key("location")
                            .query(Document("category", category.name))
                            .maxDistance(maxDistanceInMeters.toDouble())
                            .spherical(),
                    ),
                    Aggregates.limit(1),
                    Aggregates.project(Document("name", 1).append("center", 1).append("distance", 1)),
                ),
            )
            .asFlow()
            .firstOrNull()
            ?.let { document ->
                val (longitude, latitude) = document.get("center", Document::class.java).getList("coordinates", Number::class.java)
                NearbyPlace(
                    category = category,
                    name = document.getString("name"),
                    distanceInMeters = (document.get("distance") as Number).toDouble().roundToInt(),
                    latitude = latitude.toDouble(),
                    longitude = longitude.toDouble(),
                )
            }

    private fun Place.toDocument(importedAt: Date): Document {
        val outline = (outerRings + innerRings).flatten().distinct()
        val location = if (outline.isEmpty()) {
            center.toGeoJsonPoint()
        } else {
            Document("type", "MultiPoint").append("coordinates", outline.map { listOf(it.longitude, it.latitude) })
        }
        return Document("_id", id)
            .append("category", category.name)
            .append("name", name)
            .append("center", center.toGeoJsonPoint())
            .append("location", location)
            .append("importedAt", importedAt)
    }

    private fun Coordinates.toGeoJsonPoint(): Document =
        Document("type", "Point").append("coordinates", listOf(longitude, latitude))

    private companion object {
        const val BATCH_SIZE = 500
    }
}
