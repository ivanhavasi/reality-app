package cz.havasi.reality.app.rest

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.junit.jupiter.api.Test

@QuarkusTest
internal class AdminJobControllerIT {
    @Test
    fun `job list needs a signed-in admin`() {
        given().get("/api/admin/jobs").then().statusCode(401)
    }

    @Test
    fun `starting a job needs a signed-in admin`() {
        given().post("/api/admin/jobs/scrape?building=LAND&transaction=SALE").then().statusCode(401)
        given().post("/api/admin/jobs/backfill-coordinates").then().statusCode(401)
    }
}
