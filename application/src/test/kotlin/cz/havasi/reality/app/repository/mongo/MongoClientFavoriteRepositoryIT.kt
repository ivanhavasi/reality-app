package cz.havasi.reality.app.repository.mongo

import com.mongodb.client.MongoClient
import com.mongodb.client.model.Filters
import cz.havasi.reality.app.repository.mongo.TestDatabaseNames.TEST_DB_NAME
import cz.havasi.reality.app.service.repository.FavoriteRepository
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import kotlinx.coroutines.test.runTest
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@QuarkusTest
internal class MongoClientFavoriteRepositoryIT {
    @Inject
    lateinit var favorites: FavoriteRepository

    @Inject
    lateinit var mongoClient: MongoClient

    private val user = ObjectId.get().toHexString()
    private val otherUser = ObjectId.get().toHexString()

    @AfterEach
    fun cleanUp() {
        mongoClient.getDatabase(TEST_DB_NAME).getCollection("favoriteListings")
            .deleteMany(Filters.`in`("userId", ObjectId(user), ObjectId(otherUser)))
    }

    @Test
    fun `starring twice keeps one favorite with the first time`() = runTest {
        favorites.save(user, "listing")
        val first = favorites.findByUser(user).single()
        favorites.save(user, "listing")

        assertEquals(listOf(first), favorites.findByUser(user))
    }

    @Test
    fun `favorites are per user, newest first, and removed one by one`() = runTest {
        favorites.save(user, "older")
        favorites.save(user, "newer")
        favorites.save(otherUser, "older")

        assertEquals(listOf("newer", "older"), favorites.findByUser(user).map { it.listingId })

        favorites.delete(user, "older")

        assertEquals(listOf("newer"), favorites.findByUser(user).map { it.listingId })
        assertEquals(listOf("older"), favorites.findByUser(otherUser).map { it.listingId })
    }
}
