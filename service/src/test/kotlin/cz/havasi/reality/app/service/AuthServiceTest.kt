package cz.havasi.reality.app.service

import cz.havasi.reality.app.model.GoogleTokens
import cz.havasi.reality.app.service.client.GoogleAuthClient
import cz.havasi.reality.app.service.client.InvalidGoogleGrantException
import cz.havasi.reality.app.service.repository.GoogleRefreshTokenRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

internal class AuthServiceTest {
    private val google = FakeGoogleAuthClient()
    private val refreshTokens = FakeGoogleRefreshTokenRepository()
    private val service = AuthService(google, refreshTokens)

    @Test
    fun `first sign-in keeps the refresh token Google sends`() = runTest {
        google.signInTokens = GoogleTokens("access", 3599, refreshToken = "refresh", email = "a@b.cz")

        assertEquals("refresh", service.signIn("code").refreshToken)
        assertEquals("refresh", refreshTokens.find("a@b.cz"))
    }

    @Test
    fun `later sign-in without a refresh token uses the kept one`() = runTest {
        refreshTokens.save("a@b.cz", "kept")
        google.signInTokens = GoogleTokens("access", 3599, email = "a@b.cz")

        val tokens = service.signIn("code")

        assertEquals("access", tokens.accessToken)
        assertEquals("kept", tokens.refreshToken)
    }

    @Test
    fun `sign-in of a user never seen with a refresh token can't be refreshed`() = runTest {
        google.signInTokens = GoogleTokens("access", 3599, email = "new@b.cz")

        assertNull(service.signIn("code").refreshToken)
    }

    @Test
    fun `sign-in without an email is rejected`() = runTest {
        google.signInTokens = GoogleTokens("access", 3599, refreshToken = "refresh")

        assertFailsWith<InvalidGoogleGrantException> { service.signIn("code") }
    }

    @Test
    fun `refresh asks Google for a new access token`() = runTest {
        assertEquals("access-for-kept", service.refresh("kept").accessToken)
    }
}

private class FakeGoogleAuthClient : GoogleAuthClient {
    var signInTokens = GoogleTokens("access", 3599)

    override suspend fun exchangeCode(code: String): GoogleTokens = signInTokens

    override suspend fun refresh(refreshToken: String): GoogleTokens = GoogleTokens("access-for-$refreshToken", 3599)
}

private class FakeGoogleRefreshTokenRepository : GoogleRefreshTokenRepository {
    private val tokens = mutableMapOf<String, String>()

    override suspend fun find(email: String): String? = tokens[email]

    override suspend fun save(email: String, refreshToken: String) {
        tokens[email] = refreshToken
    }
}
