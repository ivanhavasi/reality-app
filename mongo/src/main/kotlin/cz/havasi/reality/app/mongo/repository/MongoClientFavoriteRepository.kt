package cz.havasi.reality.app.mongo.repository

import com.mongodb.ErrorCategory
import com.mongodb.MongoWriteException
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import cz.havasi.reality.app.model.Favorite
import cz.havasi.reality.app.mongo.DatabaseNames.DB_NAME
import cz.havasi.reality.app.mongo.DatabaseNames.FAVORITE_COLLECTION_NAME
import cz.havasi.reality.app.mongo.entity.FavoriteEntity
import cz.havasi.reality.app.service.repository.FavoriteRepository
import io.quarkus.mongodb.FindOptions
import io.quarkus.mongodb.reactive.ReactiveMongoClient
import io.smallrye.mutiny.coroutines.asFlow
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC

@ApplicationScoped
internal class MongoClientFavoriteRepository(
    reactiveMongoClient: ReactiveMongoClient,
) : FavoriteRepository {
    private val mongoCollection =
        reactiveMongoClient.getDatabase(DB_NAME).getCollection(FAVORITE_COLLECTION_NAME, FavoriteEntity::class.java)

    // a double click sends two upserts at once: the unique index lets one insert, the other finds the favorite already there
    override suspend fun save(userId: String, listingId: String) {
        try {
            mongoCollection.updateOne(
                favoriteOf(userId, listingId),
                Updates.setOnInsert("createdAt", OffsetDateTime.now(UTC)),
                UpdateOptions().upsert(true),
            ).awaitSuspending()
        } catch (e: MongoWriteException) {
            if (e.error.category != ErrorCategory.DUPLICATE_KEY) throw e
        }
    }

    override suspend fun delete(userId: String, listingId: String) {
        mongoCollection.deleteOne(favoriteOf(userId, listingId)).awaitSuspending()
    }

    override suspend fun findByUser(userId: String): List<Favorite> =
        mongoCollection
            .find(Filters.eq("userId", ObjectId(userId)), FindOptions().sort(Document("createdAt", -1)))
            .asFlow()
            .map { Favorite(it.listingId, it.createdAt) }
            .toList()

    private fun favoriteOf(userId: String, listingId: String): Bson =
        Filters.and(Filters.eq("userId", ObjectId(userId)), Filters.eq("listingId", listingId))
}
