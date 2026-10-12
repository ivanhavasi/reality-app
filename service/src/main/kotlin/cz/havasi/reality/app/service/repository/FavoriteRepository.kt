package cz.havasi.reality.app.service.repository

import cz.havasi.reality.app.model.Favorite

public interface FavoriteRepository {
    // starring a starred listing again keeps the original time
    public suspend fun save(userId: String, listingId: String)
    public suspend fun delete(userId: String, listingId: String)
    // newest first
    public suspend fun findByUser(userId: String): List<Favorite>
}
