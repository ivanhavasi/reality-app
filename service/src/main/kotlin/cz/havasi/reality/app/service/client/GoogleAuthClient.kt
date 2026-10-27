package cz.havasi.reality.app.service.client

import cz.havasi.reality.app.model.GoogleTokens

public class InvalidGoogleGrantException(message: String) : RuntimeException(message)

public interface GoogleAuthClient {
    // both throw InvalidGoogleGrantException when Google rejects the code or the refresh token
    public suspend fun exchangeCode(code: String): GoogleTokens
    public suspend fun refresh(refreshToken: String): GoogleTokens
}
