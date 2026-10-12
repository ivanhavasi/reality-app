package cz.havasi.reality.app.rest.client

import cz.havasi.reality.app.model.Coordinates
import cz.havasi.reality.app.model.Place
import cz.havasi.reality.app.model.PlaceCategory
import cz.havasi.reality.app.rest.client.api.OverpassApi
import cz.havasi.reality.app.rest.client.model.OverpassElement
import cz.havasi.reality.app.rest.client.model.OverpassPoint
import cz.havasi.reality.app.service.client.PlacesClient
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient

@ApplicationScoped
internal class RestOverpassClient(
    @RestClient private val overpassApi: OverpassApi,
) : PlacesClient {
    override suspend fun findPlaces(category: PlaceCategory, southWest: Coordinates, northEast: Coordinates): List<Place> {
        val box = "${southWest.latitude},${southWest.longitude},${northEast.latitude},${northEast.longitude}"
        // parks need their outline, everything else is one point
        val output = if (category == PlaceCategory.PARK) "out geom;" else "out center;"
        return overpassApi.query("[out:json][timeout:$QUERY_TIMEOUT_SECONDS];${category.filter()}($box);$output")
            .elements
            .mapNotNull { it.toPlace(category) }
    }

    private fun OverpassElement.toPlace(category: PlaceCategory): Place? {
        val center = when {
            lat != null && lon != null -> Coordinates(lat, lon)
            center != null -> Coordinates(center.lat, center.lon)
            bounds != null -> Coordinates((bounds.minlat + bounds.maxlat) / 2, (bounds.minlon + bounds.maxlon) / 2)
            else -> return null
        }
        val place = Place(id = "$type/$id", category = category, name = tags?.get("name")?.trim()?.ifBlank { null }, center = center)
        if (category != PlaceCategory.PARK) return place

        return when (type) {
            "way" -> place.copy(outerRings = listOfNotNull(geometry?.toCoordinates()))
            "relation" -> {
                val ways = members.orEmpty().filter { it.type == "way" }
                place.copy(
                    outerRings = joinRings(ways.filter { it.role != "inner" }.mapNotNull { it.geometry?.toCoordinates() }),
                    innerRings = joinRings(ways.filter { it.role == "inner" }.mapNotNull { it.geometry?.toCoordinates() }),
                )
            }

            else -> null // a park mapped as a single point has no area to judge it by
        }
    }

    private fun List<OverpassPoint?>.toCoordinates(): List<Coordinates>? =
        filterNotNull().map { Coordinates(it.lat, it.lon) }.takeIf { it.size >= 2 }

    private fun PlaceCategory.filter(): String = when (this) {
        PlaceCategory.SUPERMARKET -> """nwr["shop"="supermarket"]"""
        PlaceCategory.PHARMACY -> """nwr["amenity"="pharmacy"]"""
        PlaceCategory.PARK -> """nwr["leisure"="park"]"""
        PlaceCategory.PLAYGROUND -> """nwr["leisure"="playground"]"""
        PlaceCategory.METRO -> """node["railway"="station"]["station"="subway"]"""
        PlaceCategory.TRAM -> """node["railway"="tram_stop"]"""
        PlaceCategory.TRAIN -> """node["railway"~"^(station|halt)${'$'}"]["station"!~"subway|funicular|light_rail|monorail|tram"]"""
        PlaceCategory.KINDERGARTEN -> """nwr["amenity"="kindergarten"]"""
        PlaceCategory.SCHOOL -> """nwr["amenity"="school"]"""
        PlaceCategory.HOSPITAL -> """nwr["amenity"="hospital"]"""
        PlaceCategory.CINEMA -> """nwr["amenity"="cinema"]"""
        PlaceCategory.THEATRE -> """nwr["amenity"="theatre"]"""
    }

    private companion object {
        const val QUERY_TIMEOUT_SECONDS = 90
    }
}

// a large park's boundary is split into several ways; this chains ways that share an end into rings
internal fun joinRings(pieces: List<List<Coordinates>>): List<List<Coordinates>> {
    val remaining = pieces.toMutableList()
    val rings = mutableListOf<List<Coordinates>>()
    while (remaining.isNotEmpty()) {
        val ring = remaining.removeAt(0).toMutableList()
        while (ring.first() != ring.last()) {
            val index = remaining.indexOfFirst { it.first() == ring.last() || it.last() == ring.last() }
            if (index < 0) break // the rest of the ring lies outside the queried box
            val next = remaining.removeAt(index)
            ring += (if (next.first() == ring.last()) next else next.reversed()).drop(1)
        }
        rings += ring
    }
    return rings
}
