package cz.havasi.reality.app.rest

import com.mongodb.client.MongoClient
import com.mongodb.client.model.Filters
import cz.havasi.reality.app.AbstractIT
import cz.havasi.reality.app.helper.ApartmentHelper.APARTMENT
import cz.havasi.reality.app.repository.mongo.TestDatabaseNames.TEST_DB_NAME
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.SecurityAttribute
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured.given
import jakarta.inject.Inject
import kotlinx.coroutines.test.runTest
import org.bson.types.ObjectId
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.empty
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@QuarkusTest
internal class FavoriteControllerIT : AbstractIT() {
    @Inject
    lateinit var mongoClient: MongoClient

    @AfterEach
    fun removeFavorites() {
        mongoClient.getDatabase(TEST_DB_NAME).getCollection("favoriteListings").deleteMany(Filters.eq("userId", ObjectId(USER_ID)))
    }

    @Test
    fun `favorites need a signed-in user`() {
        given().get("/api/users/$USER_ID/favorites").then().statusCode(401)
        given().put("/api/users/$USER_ID/favorites/${APARTMENT.id}").then().statusCode(401)
        given().delete("/api/users/$USER_ID/favorites/${APARTMENT.id}").then().statusCode(401)
    }

    @Test
    @TestSecurity(user = "tester", roles = ["USER"], attributes = [SecurityAttribute(key = "id", value = USER_ID)])
    fun `user stars a listing, finds it among favorites and removes it`() = runTest {
        apartmentRepository.save(APARTMENT)

        given().put("/api/users/$USER_ID/favorites/${APARTMENT.id}").then().statusCode(204)
        given().put("/api/users/$USER_ID/favorites/${APARTMENT.id}").then().statusCode(204)
        given().get("/api/users/$USER_ID/favorites").then()
            .statusCode(200)
            .body("listing.id", contains(APARTMENT.id))
            .body("listing.name", contains(APARTMENT.name))

        given().delete("/api/users/$USER_ID/favorites/${APARTMENT.id}").then().statusCode(204)
        given().get("/api/users/$USER_ID/favorites").then().statusCode(200).body("$", empty<Any>())
    }

    @Test
    @TestSecurity(user = "tester", roles = ["USER"], attributes = [SecurityAttribute(key = "id", value = USER_ID)])
    fun `unknown listing can't be starred`() {
        given().put("/api/users/$USER_ID/favorites/missing").then().statusCode(404)
    }

    @Test
    @TestSecurity(user = "tester", roles = ["USER"], attributes = [SecurityAttribute(key = "id", value = USER_ID)])
    fun `another user's favorites are off limits`() {
        given().get("/api/users/${ObjectId.get().toHexString()}/favorites").then().statusCode(403)
    }

    private companion object {
        const val USER_ID = "65f0c0ffee0000000000f00d"
    }
}
