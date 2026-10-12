package cz.havasi.reality.app.mongo.entity

import io.quarkus.runtime.annotations.RegisterForReflection
import java.time.OffsetDateTime

// one document per street and city; no segments means the geocoder didn't know the street
@RegisterForReflection
internal data class GeocodedStreetEntity(
    val _id: String, // normalized "street|city"
    val segments: List<StreetSegmentEntity>,
    val createdAt: OffsetDateTime,
)

@RegisterForReflection
internal data class StreetSegmentEntity(
    val latitude: Double,
    val longitude: Double,
    val description: String,
)
