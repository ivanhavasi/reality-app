package cz.havasi.reality.app.rest.client

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import cz.havasi.reality.app.model.GoogleTokens
import cz.havasi.reality.app.rest.client.api.GoogleOAuthApi
import cz.havasi.reality.app.rest.client.model.GoogleTokenResponse
import cz.havasi.reality.app.service.client.GoogleAuthClient
import cz.havasi.reality.app.service.client.InvalidGoogleGrantException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.WebApplicationException
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.util.Base64
import java.util.Optional

@ApplicationScoped
internal class RestGoogleAuthClient(
    @RestClient private val googleOAuthApi: GoogleOAuthApi,
    @ConfigProperty(name = "quarkus.oidc.client-id") private val clientId: String,
    @ConfigProperty(name = "reality.google.client-secret") private val clientSecret: Optional<String>,
) : GoogleAuthClient {
    override suspend fun exchangeCode(code: String): GoogleTokens =
        call { googleOAuthApi.exchangeCode(code, clientId, secret(), POPUP_REDIRECT_URI, "authorization_code") }

    override suspend fun refresh(refreshToken: String): GoogleTokens =
        call { googleOAuthApi.refresh(refreshToken, clientId, secret(), "refresh_token") }

    private fun secret(): String =
        clientSecret.orElseThrow { IllegalStateException("GOOGLE_CLIENT_SECRET is not set") }

    // Google answers 400 invalid_grant for a used or expired code and for a revoked refresh token
    private suspend fun call(request: suspend () -> GoogleTokenResponse): GoogleTokens {
        val response = try {
            request()
        } catch (e: WebApplicationException) {
            if (e.response.status in 400..499) throw InvalidGoogleGrantException("Google rejected the grant (${e.response.status})")
            throw e
        }
        return GoogleTokens(
            accessToken = response.accessToken,
            expiresInSeconds = response.expiresIn,
            refreshToken = response.refreshToken,
            email = response.idToken?.let(::emailOf),
        )
    }

    // the id token comes straight from Google over TLS, so its signature needn't be checked
    private fun emailOf(idToken: String): String? {
        val payload = idToken.split('.').getOrNull(1) ?: return null
        return objectMapper.readTree(Base64.getUrlDecoder().decode(payload)).get("email")?.asText()
    }

    private companion object {
        // the browser's popup sign-in (@react-oauth/google auth-code flow) uses this instead of a real redirect
        const val POPUP_REDIRECT_URI = "postmessage"
        val objectMapper = jacksonObjectMapper()
    }
}
