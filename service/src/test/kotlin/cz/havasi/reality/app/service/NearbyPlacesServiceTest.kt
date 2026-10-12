package cz.havasi.reality.app.service

import cz.havasi.reality.app.model.PlaceCategory
import cz.havasi.reality.app.model.type.ProviderType
import cz.havasi.reality.app.service.util.densified
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class NearbyPlacesServiceTest {
    private val repository = FakePlaceRepository()
    private val service = NearbyPlacesService(repository)
    private val listing = apartment("listing", ProviderType.SREALITY, 60.0, 8_000_000.0).let {
        it.copy(locality = it.locality.copy(latitude = 50.0755, longitude = 14.4378))
    }

    @Test
    fun `nearest of each category in category order, farther ones and those out of reach left out`() = runTest {
        repository.stored[PlaceCategory.THEATRE] = listOf(point("Divadlo", PlaceCategory.THEATRE, 50.0800, 14.4378))
        repository.stored[PlaceCategory.SUPERMARKET] = listOf(
            point("Far", PlaceCategory.SUPERMARKET, 50.0855, 14.4378),
            point("Near", PlaceCategory.SUPERMARKET, 50.0765, 14.4378),
        )
        repository.stored[PlaceCategory.HOSPITAL] = listOf(point("Brno", PlaceCategory.HOSPITAL, 49.1951, 16.6068))

        val nearby = service.find(listing)

        assertEquals(listOf("Near", "Divadlo"), nearby.map { it.name })
        assertEquals(111, nearby.first().distanceInMeters)
    }

    @Test
    fun `park is measured to its edge`() = runTest {
        // 400 m wide, its south edge 100 m north of the listing; densified like the import does
        val park = squarePark("Park", 50.0755 + 100 / 111_195.0, 14.4378 - 0.0028, 400.0)
        repository.stored[PlaceCategory.PARK] = listOf(park.copy(outerRings = park.outerRings.map { it.densified(25.0) }))

        assertEquals(100, service.find(listing).single().distanceInMeters)
    }

    @Test
    fun `listing without coordinates has nothing nearby`() = runTest {
        repository.stored[PlaceCategory.SUPERMARKET] = listOf(point("Near", PlaceCategory.SUPERMARKET, 50.0765, 14.4378))

        assertEquals(emptyList(), service.find(listing.copy(locality = listing.locality.copy(latitude = null, longitude = null))))
    }
}
