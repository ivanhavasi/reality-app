package cz.havasi.reality.app.service

import cz.havasi.reality.app.model.FavoriteListing
import cz.havasi.reality.app.service.repository.ApartmentRepository
import cz.havasi.reality.app.service.repository.FavoriteRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
public class FavoriteService(
    private val favoriteRepository: FavoriteRepository,
    private val apartmentRepository: ApartmentRepository,
) {
    public suspend fun star(userId: String, listingId: String) {
        if (apartmentRepository.findByIds(listOf(listingId)).isEmpty()) {
            throw NoSuchElementException("Apartment with id $listingId not found")
        }
        favoriteRepository.save(userId, listingId)
    }

    public suspend fun unstar(userId: String, listingId: String): Unit =
        favoriteRepository.delete(userId, listingId)

    // newest first; a listing that is no longer stored is left out
    public suspend fun getFavorites(userId: String): List<FavoriteListing> {
        val favorites = favoriteRepository.findByUser(userId)
        if (favorites.isEmpty()) return emptyList()
        val listings = apartmentRepository.findByIds(favorites.map { it.listingId }).associateBy { it.id }
        return favorites.mapNotNull { favorite -> listings[favorite.listingId]?.let { FavoriteListing(it, favorite.starredAt) } }
    }
}
