package cz.havasi.reality.app.repository.mongo

import com.mongodb.client.MongoClient
import com.mongodb.client.model.Filters
import cz.havasi.reality.app.repository.mongo.TestDatabaseNames.TEST_DB_NAME
import cz.havasi.reality.app.service.repository.GoogleRefreshTokenRepository
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import kotlinx.coroutines.test.runTest
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@QuarkusTest
internal class MongoClientGoogleRefreshTokenRepositoryIT {
    @Inject
    lateinit var refreshTokens: GoogleRefreshTokenRepository

    @Inject
    lateinit var mongoClient: MongoClient

    private val email = "it-${ObjectId.get().toHexString()}@havasi.me"

    @AfterEach
    fun cleanUp() {
        mongoClient.getDatabase(TEST_DB_NAME).getCollection("googleRefreshTokens").deleteMany(Filters.eq("_id", email))
    }

    @Test
    fun `the latest refresh token of a user is kept`() = runTest {
        assertNull(refreshTokens.find(email))

        refreshTokens.save(email, "first")
        refreshTokens.save(email, "second")

        assertEquals("second", refreshTokens.find(email))
    }
}
