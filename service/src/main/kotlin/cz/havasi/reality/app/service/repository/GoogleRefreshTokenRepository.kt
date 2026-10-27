package cz.havasi.reality.app.service.repository

public interface GoogleRefreshTokenRepository {
    public suspend fun find(email: String): String?
    public suspend fun save(email: String, refreshToken: String)
}
