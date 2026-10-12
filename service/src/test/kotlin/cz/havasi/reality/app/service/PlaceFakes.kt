package cz.havasi.reality.app.service

import cz.havasi.reality.app.model.Coordinates
import cz.havasi.reality.app.model.NearbyPlace
import cz.havasi.reality.app.model.Place
import cz.havasi.reality.app.model.PlaceCategory
import cz.havasi.reality.app.service.client.PlacesClient
import cz.havasi.reality.app.service.repository.PlaceRepository
import cz.havasi.reality.app.service.util.distanceInMetersTo

internal class FakePlacesClient(
    private val places: Map<PlaceCategory, List<Place>> = emptyMap(),
    // how many calls fail before one succeeds, per category
    failures: Map<PlaceCategory, Int> = emptyMap(),
) : PlacesClient {
    private val failuresLeft = failures.toMutableMap()
    val asked = mutableListOf<PlaceCategory>()

    override suspend fun findPlaces(category: PlaceCategory, southWest: Coordinates, northEast: Coordinates): List<Place> {
        asked += category
        val left = failuresLeft[category] ?: 0
        if (left > 0) {
            failuresLeft[category] = left - 1
            error("Overpass is busy")
        }
        return places[category].orEmpty()
    }
}

// nearest by any point of the outline, like the 2dsphere index does for a MultiPoint
internal class FakePlaceRepository : PlaceRepository {
    val stored = mutableMapOf<PlaceCategory, List<Place>>()

    override suspend fun replaceCategory(category: PlaceCategory, places: List<Place>) {
        stored[category] = places
    }

    override suspend fun findNearest(category: PlaceCategory, coordinates: Coordinates, maxDistanceInMeters: Int): NearbyPlace? =
        stored[category].orEmpty()
            .map { place ->
                val points = (place.outerRings + place.innerRings).flatten().ifEmpty { listOf(place.center) }
                place to points.minOf { coordinates.distanceInMetersTo(it) }
            }
            .filter { (_, distance) -> distance <= maxDistanceInMeters }
            .minByOrNull { (_, distance) -> distance }
            ?.let { (place, distance) ->
                NearbyPlace(category, place.name, distance.toInt(), place.center.latitude, place.center.longitude)
            }
}

// a park needs an outline to pass the size rule
internal fun onePlaceInEveryCategory(): Map<PlaceCategory, List<Place>> =
    PlaceCategory.entries.associateWith { category ->
        listOf(if (category == PlaceCategory.PARK) squarePark("park", 50.07, 14.43, 100.0) else point("${category.name}-1", category, 50.07, 14.43))
    }

internal fun point(id: String, category: PlaceCategory, latitude: Double, longitude: Double, name: String? = id) =
    Place(id = id, category = category, name = name, center = Coordinates(latitude, longitude))

// a square park of the given side, its south-west corner at the point
internal fun squarePark(id: String, latitude: Double, longitude: Double, sideInMeters: Double): Place {
    val latitudeStep = sideInMeters / 111_195.0
    val longitudeStep = sideInMeters / (111_195.0 * Math.cos(Math.toRadians(latitude)))
    val ring = listOf(
        Coordinates(latitude, longitude),
        Coordinates(latitude, longitude + longitudeStep),
        Coordinates(latitude + latitudeStep, longitude + longitudeStep),
        Coordinates(latitude + latitudeStep, longitude),
        Coordinates(latitude, longitude),
    )
    return Place(
        id = id,
        category = PlaceCategory.PARK,
        name = id,
        center = Coordinates(latitude + latitudeStep / 2, longitude + longitudeStep / 2),
        outerRings = listOf(ring),
    )
}
