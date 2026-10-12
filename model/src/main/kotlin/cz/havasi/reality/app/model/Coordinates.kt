package cz.havasi.reality.app.model

import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
public data class Coordinates(
    val latitude: Double,
    val longitude: Double,
)

// one piece of a street as the geocoder knows it; a long street comes back as several, one per city part
@RegisterForReflection
public data class StreetSegment(
    val coordinates: Coordinates,
    // the geocoder's full name of the place, e.g. "Budějovická, Zelená Liška, Krč, Praha 4, ..., Praha, 140 02, Česko"
    val description: String,
)
