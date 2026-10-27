package cz.havasi.reality.app.rest.client

import cz.havasi.reality.app.rest.client.api.GoogleOAuthApi
import cz.havasi.reality.app.rest.client.model.GoogleTokenResponse
import cz.havasi.reality.app.service.client.InvalidGoogleGrantException
import jakarta.ws.rs.WebApplicationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.Base64
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

internal class RestGoogleAuthClientTest {
    private val requests = mutableListOf<List<String>>()
    private var response = GoogleTokenResponse("access", 3599)
    private var failure: Exception? = null
    private val api = object : GoogleOAuthApi {
        override suspend fun exchangeCode(code: String, clientId: String, clientSecret: String, redirectUri: String, grantType: String) =
            answer(listOf(code, clientId, clientSecret, redirectUri, grantType))

        override suspend fun refresh(refreshToken: String, clientId: String, clientSecret: String, grantType: String) =
            answer(listOf(refreshToken, clientId, clientSecret, grantType))

        private fun answer(request: List<String>): GoogleTokenResponse {
            requests += request
            failure?.let { throw it }
            return response
        }
    }
    private val client = RestGoogleAuthClient(api, "client-id", Optional.of("secret"))

    private fun idToken(payload: String) =
        "header.${Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())}.signature"

    @Test
    fun `code is exchanged for the popup sign-in and the email is read from the id token`() = runTest {
        response = GoogleTokenResponse("access", 3599, "refresh", idToken("""{"email":"a@b.cz","sub":"1"}"""))

        val tokens = client.exchangeCode("code")

        assertEquals(listOf("code", "client-id", "secret", "postmessage", "authorization_code"), requests.single())
        assertEquals("access", tokens.accessToken)
        assertEquals(3599, tokens.expiresInSeconds)
        assertEquals("refresh", tokens.refreshToken)
        assertEquals("a@b.cz", tokens.email)
    }

    @Test
    fun `refresh sends the refresh token`() = runTest {
        val tokens = client.refresh("refresh")

        assertEquals(listOf("refresh", "client-id", "secret", "refresh_token"), requests.single())
        assertEquals("access", tokens.accessToken)
        assertNull(tokens.email)
    }

    @Test
    fun `rejected grant is reported as invalid`() = runTest {
        failure = WebApplicationException(400)

        assertFailsWith<InvalidGoogleGrantException> { client.refresh("revoked") }
    }

    @Test
    fun `Google outage is not mistaken for a rejected grant`() = runTest {
        failure = WebApplicationException(503)

        assertFailsWith<WebApplicationException> { client.refresh("refresh") }
    }

    @Test
    fun `missing client secret fails loudly`() = runTest {
        val unconfigured = RestGoogleAuthClient(api, "client-id", Optional.empty())

        assertFailsWith<IllegalStateException> { unconfigured.refresh("refresh") }
    }
}
