package cz.havasi.reality.app.rest

import com.mongodb.client.MongoClient
import com.mongodb.client.model.Filters
import cz.havasi.reality.app.AbstractIT
import cz.havasi.reality.app.helper.ApartmentHelper.APARTMENT
import cz.havasi.reality.app.model.Coordinates
import cz.havasi.reality.app.model.Place
import cz.havasi.reality.app.model.PlaceCategory
import cz.havasi.reality.app.repository.mongo.TestDatabaseNames.TEST_DB_NAME
import cz.havasi.reality.app.service.repository.PlaceRepository
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.inject.Inject
import kotlinx.coroutines.test.runTest
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.empty
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@QuarkusTest
internal class NearbyPlacesIT : AbstractIT() {
    @Inject
    lateinit var places: PlaceRepository

    @Inject
    lateinit var mongoClient: MongoClient

    @AfterEach
    fun removePlaces() {
        mongoClient.getDatabase(TEST_DB_NAME).getCollection("places").deleteMany(Filters.empty())
    }

    @Test
    fun `public listing detail gets the nearest places in category order`() = runTest {
        val listing = APARTMENT.copy(id = "nearby-listing", fingerprint = "nearby-listing", locality = APARTMENT.locality.copy(latitude = 50.0755, longitude = 14.4378))
        apartmentRepository.save(listing)
        places.replaceCategory(PlaceCategory.THEATRE, listOf(Place("way/1", PlaceCategory.THEATRE, "Divadlo na Vinohradech", Coordinates(50.0760, 14.4378))))
        places.replaceCategory(PlaceCategory.SUPERMARKET, listOf(Place("node/2", PlaceCategory.SUPERMARKET, "Albert", Coordinates(50.0765, 14.4378))))

        given().get("/api/real-estates/nearby-listing/nearby")
            .then()
            .statusCode(200)
            .body("category", contains("SUPERMARKET", "THEATRE"))
            .body("name", contains("Albert", "Divadlo na Vinohradech"))
            .body("distanceInMeters", contains(111, 56))
    }

    @Test
    fun `listing without coordinates has nothing nearby`() = runTest {
        apartmentRepository.save(APARTMENT.copy(id = "nowhere", fingerprint = "nowhere", locality = APARTMENT.locality.copy(latitude = null, longitude = null)))

        given().get("/api/real-estates/nowhere/nearby").then().statusCode(200).body("$", empty<Any>())
    }
}
