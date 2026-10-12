package cz.havasi.reality.app.repository.mongo

import com.mongodb.client.MongoClient
import com.mongodb.client.model.Filters
import cz.havasi.reality.app.model.Coordinates
import cz.havasi.reality.app.model.StreetSegment
import cz.havasi.reality.app.repository.mongo.TestDatabaseNames.TEST_DB_NAME
import cz.havasi.reality.app.service.repository.GeocodingCacheRepository
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import kotlinx.coroutines.test.runTest
import org.bson.types.ObjectId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@QuarkusTest
internal class MongoClientGeocodingCacheRepositoryIT {
    @Inject
    lateinit var cache: GeocodingCacheRepository

    @Inject
    lateinit var mongoClient: MongoClient

    private val known = "it-known-${ObjectId.get().toHexString()}|praha"
    private val unknown = "it-unknown-${ObjectId.get().toHexString()}|praha"

    @AfterEach
    fun cleanUp() {
        mongoClient.getDatabase(TEST_DB_NAME).getCollection("geocodingCache").deleteMany(Filters.`in`("_id", known, unknown))
    }

    @Test
    fun `street pieces and unknown streets both come back from the cache`() = runTest {
        val segments = listOf(
            StreetSegment(Coordinates(50.0770, 14.4884), "Vinohradská, Třebešín, Praha"),
            StreetSegment(Coordinates(50.0778, 14.4610), "Vinohradská, Žižkov, Praha"),
        )

        assertNull(cache.findStreet(known))
        cache.saveStreet(known, segments)
        cache.saveStreet(unknown, emptyList())

        assertEquals(segments, cache.findStreet(known))
        assertEquals(emptyList<StreetSegment>(), cache.findStreet(unknown))
    }
}
