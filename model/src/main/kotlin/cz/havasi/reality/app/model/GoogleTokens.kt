package cz.havasi.reality.app.model

// refreshToken: Google sends one only the first time a user allows offline access
public data class GoogleTokens(
    val accessToken: String,
    val expiresInSeconds: Long,
    val refreshToken: String? = null,
    val email: String? = null,
)
