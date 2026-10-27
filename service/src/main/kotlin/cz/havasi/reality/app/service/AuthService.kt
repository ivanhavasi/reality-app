package cz.havasi.reality.app.service

import cz.havasi.reality.app.model.GoogleTokens
import cz.havasi.reality.app.service.client.GoogleAuthClient
import cz.havasi.reality.app.service.client.InvalidGoogleGrantException
import cz.havasi.reality.app.service.repository.GoogleRefreshTokenRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
public class AuthService(
    private val googleAuthClient: GoogleAuthClient,
    private val refreshTokenRepository: GoogleRefreshTokenRepository,
) {
    // Google sends a refresh token only on the first sign-in, so it is kept for later sign-ins and other devices
    public suspend fun signIn(code: String): GoogleTokens {
        val tokens = googleAuthClient.exchangeCode(code)
        val email = tokens.email ?: throw InvalidGoogleGrantException("Google sent no email")
        val refreshToken = tokens.refreshToken?.also { refreshTokenRepository.save(email, it) }
            ?: refreshTokenRepository.find(email)
        return tokens.copy(refreshToken = refreshToken)
    }

    public suspend fun refresh(refreshToken: String): GoogleTokens =
        googleAuthClient.refresh(refreshToken)
}
