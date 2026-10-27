package cz.havasi.reality.app.rest

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test

@QuarkusTest
internal class AuthControllerIT {
    @Test
    fun `refresh without the cookie is unauthorized`() {
        given().post("/api/auth/refresh").then().statusCode(401)
    }

    @Test
    fun `logout clears the refresh cookie`() {
        given().post("/api/auth/logout").then()
            .statusCode(204)
            .header("Set-Cookie", containsString("reality_refresh=;"))
            .header("Set-Cookie", containsString("Max-Age=0"))
    }
}
