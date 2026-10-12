package cz.havasi.reality.app.repository.mongo

import com.mongodb.client.MongoClient
import com.mongodb.client.model.Filters
import cz.havasi.reality.app.model.Coordinates
import cz.havasi.reality.app.model.NearbyPlace
import cz.havasi.reality.app.model.Place
import cz.havasi.reality.app.model.PlaceCategory
import cz.havasi.reality.app.repository.mongo.TestDatabaseNames.TEST_DB_NAME
import cz.havasi.reality.app.service.repository.PlaceRepository
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@QuarkusTest
internal class MongoClientPlaceRepositoryIT {
    @Inject
    lateinit var places: PlaceRepository

    @Inject
    lateinit var mongoClient: MongoClient

    private val listing = Coordinates(50.0755, 14.4378)
    private val metersNorth = { meters: Int -> listing.latitude + meters / 111_195.0 }

    @AfterEach
    fun cleanUp() {
        mongoClient.getDatabase(TEST_DB_NAME).getCollection("places").deleteMany(Filters.empty())
    }

    @Test
    fun `nearest place of the category, a park measured to its outline`() = runTest {
        places.replaceCategory(
            PlaceCategory.SUPERMARKET,
            listOf(
                Place("node/1", PlaceCategory.SUPERMARKET, "Far", Coordinates(metersNorth(500), listing.longitude)),
                Place("node/2", PlaceCategory.SUPERMARKET, "Near", Coordinates(metersNorth(200), listing.longitude)),
            ),
        )
        // its middle is 1 km away, its edge 150 m
        val edge = (0..10).map { Coordinates(metersNorth(150), listing.longitude - 0.005 + it * 0.001) }
        places.replaceCategory(
            PlaceCategory.PARK,
            listOf(Place("way/3", PlaceCategory.PARK, "Riegrovy sady", Coordinates(metersNorth(1_000), listing.longitude), outerRings = listOf(edge))),
        )

        assertEquals(
            NearbyPlace(PlaceCategory.SUPERMARKET, "Near", 200, metersNorth(200), listing.longitude),
            places.findNearest(PlaceCategory.SUPERMARKET, listing, 10_000),
        )
        assertEquals(150, places.findNearest(PlaceCategory.PARK, listing, 10_000)?.distanceInMeters)
        assertEquals("Riegrovy sady", places.findNearest(PlaceCategory.PARK, listing, 10_000)?.name)
        assertNull(places.findNearest(PlaceCategory.SUPERMARKET, listing, 100))
        assertNull(places.findNearest(PlaceCategory.CINEMA, listing, 10_000))
    }

    @Test
    fun `replacing a category drops places that are gone and keeps other categories`() = runTest {
        places.replaceCategory(PlaceCategory.TRAM, listOf(Place("node/1", PlaceCategory.TRAM, "Old", Coordinates(metersNorth(100), listing.longitude))))
        places.replaceCategory(PlaceCategory.METRO, listOf(Place("node/2", PlaceCategory.METRO, "Muzeum", Coordinates(metersNorth(300), listing.longitude))))

        places.replaceCategory(PlaceCategory.TRAM, listOf(Place("node/3", PlaceCategory.TRAM, "New", Coordinates(metersNorth(400), listing.longitude))))

        assertEquals("New", places.findNearest(PlaceCategory.TRAM, listing, 10_000)?.name)
        assertEquals("Muzeum", places.findNearest(PlaceCategory.METRO, listing, 10_000)?.name)
    }
}
