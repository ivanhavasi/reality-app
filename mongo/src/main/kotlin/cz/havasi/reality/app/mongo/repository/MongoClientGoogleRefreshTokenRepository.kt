package cz.havasi.reality.app.mongo.repository

import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import cz.havasi.reality.app.mongo.DatabaseNames.DB_NAME
import cz.havasi.reality.app.mongo.DatabaseNames.GOOGLE_REFRESH_TOKEN_COLLECTION_NAME
import cz.havasi.reality.app.mongo.entity.GoogleRefreshTokenEntity
import cz.havasi.reality.app.service.repository.GoogleRefreshTokenRepository
import io.quarkus.mongodb.reactive.ReactiveMongoClient
import io.smallrye.mutiny.coroutines.asFlow
import io.smallrye.mutiny.coroutines.awaitSuspending
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.flow.firstOrNull
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC

@ApplicationScoped
internal class MongoClientGoogleRefreshTokenRepository(
    reactiveMongoClient: ReactiveMongoClient,
) : GoogleRefreshTokenRepository {
    private val mongoCollection = reactiveMongoClient.getDatabase(DB_NAME)
        .getCollection(GOOGLE_REFRESH_TOKEN_COLLECTION_NAME, GoogleRefreshTokenEntity::class.java)

    override suspend fun find(email: String): String? =
        mongoCollection.find(Filters.eq("_id", email), GoogleRefreshTokenEntity::class.java)
            .asFlow()
            .firstOrNull()
            ?.refreshToken

    override suspend fun save(email: String, refreshToken: String) {
        mongoCollection.replaceOne(
            Filters.eq("_id", email),
            GoogleRefreshTokenEntity(_id = email, refreshToken = refreshToken, updatedAt = OffsetDateTime.now(UTC)),
            ReplaceOptions().upsert(true),
        ).awaitSuspending()
    }
}
