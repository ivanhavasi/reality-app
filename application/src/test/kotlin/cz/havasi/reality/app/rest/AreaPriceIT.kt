package cz.havasi.reality.app.rest

import cz.havasi.reality.app.AbstractIT
import cz.havasi.reality.app.helper.ApartmentHelper.APARTMENT
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import kotlinx.coroutines.test.runTest
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
internal class AreaPriceIT : AbstractIT() {
    private fun listing(id: String, latitude: Double, pricePerM2: Double) =
        APARTMENT.copy(
            id = id,
            fingerprint = id,
            pricePerM2 = pricePerM2,
            price = pricePerM2 * APARTMENT.sizeInM2,
            locality = APARTMENT.locality.copy(latitude = latitude, longitude = 14.44),
        )

    @Test
    fun `public listing detail gets its price against the area`() = runTest {
        apartmentRepository.save(listing("area-subject", 50.08, 117_500.0))
        repeat(10) { apartmentRepository.save(listing("area-neighbour-$it", 50.081, 125_000.0)) }

        given().get("/api/real-estates/area-subject/area-price")
            .then()
            .statusCode(200)
            .body("differencePercent", equalTo(-6))
            .body("areaPricePerM2", equalTo(125_000.0f))
            .body("comparableCount", equalTo(10))
            .body("radiusInMeters", equalTo(500))
    }

    @Test
    fun `listing without enough neighbours gets no content`() = runTest {
        apartmentRepository.save(listing("area-alone", 50.08, 117_500.0))

        given().get("/api/real-estates/area-alone/area-price").then().statusCode(204)
    }
}
