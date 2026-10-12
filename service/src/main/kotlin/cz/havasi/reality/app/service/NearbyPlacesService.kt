package cz.havasi.reality.app.service

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.Coordinates
import cz.havasi.reality.app.model.NearbyPlace
import cz.havasi.reality.app.model.PlaceCategory
import cz.havasi.reality.app.service.repository.PlaceRepository
import jakarta.enterprise.context.ApplicationScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@ApplicationScoped
public class NearbyPlacesService(
    private val placeRepository: PlaceRepository,
) {
    // the nearest place of each category, in category order; categories without one in reach are left out
    public suspend fun find(listing: Apartment): List<NearbyPlace> {
        val latitude = listing.locality.latitude ?: return emptyList()
        val longitude = listing.locality.longitude ?: return emptyList()
        val center = Coordinates(latitude, longitude)
        return coroutineScope {
            PlaceCategory.entries
                .map { category -> async { placeRepository.findNearest(category, center, MAX_DISTANCE_IN_METERS) } }
                .awaitAll()
                .filterNotNull()
        }
    }
}

// places are imported for Prague only: anything farther says nothing about a listing outside it
private const val MAX_DISTANCE_IN_METERS = 10_000
