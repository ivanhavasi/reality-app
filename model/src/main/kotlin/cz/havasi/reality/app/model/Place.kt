package cz.havasi.reality.app.model

import io.quarkus.runtime.annotations.RegisterForReflection

// the order is the order the listing detail shows them in
@RegisterForReflection
public enum class PlaceCategory {
    SUPERMARKET,
    PHARMACY,
    PARK,
    PLAYGROUND,
    METRO,
    TRAM,
    TRAIN,
    KINDERGARTEN,
    SCHOOL,
    HOSPITAL,
    CINEMA,
    THEATRE,
}

@RegisterForReflection
public data class Place(
    val id: String, // OpenStreetMap element, e.g. "way/123"
    val category: PlaceCategory,
    val name: String?,
    val center: Coordinates,
    // parks only: boundaries and holes, so the distance is to the park's edge rather than its middle
    val outerRings: List<List<Coordinates>> = emptyList(),
    val innerRings: List<List<Coordinates>> = emptyList(),
)

@RegisterForReflection
public data class NearbyPlace(
    val category: PlaceCategory,
    val name: String?,
    // straight line; to a park's edge
    val distanceInMeters: Int,
    val latitude: Double,
    val longitude: Double,
)

@RegisterForReflection
public data class PlacesImportProgress(
    val categoriesImported: Int = 0,
    val placesImported: Int = 0,
)
