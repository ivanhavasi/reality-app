package cz.havasi.reality.app.rest.client.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.quarkus.runtime.annotations.RegisterForReflection

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
internal data class OverpassResponse(
    val elements: List<OverpassElement> = emptyList(),
)

// a node has lat/lon; a way or relation has center (out center) or geometry and members (out geom)
@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
internal data class OverpassElement(
    val type: String,
    val id: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassPoint? = null,
    val bounds: OverpassBounds? = null,
    // a point is null where the node lies outside the queried box
    val geometry: List<OverpassPoint?>? = null,
    val members: List<OverpassMember>? = null,
    val tags: Map<String, String>? = null,
)

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
internal data class OverpassMember(
    val type: String,
    val role: String? = null,
    val geometry: List<OverpassPoint?>? = null,
)

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
internal data class OverpassPoint(
    val lat: Double,
    val lon: Double,
)

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
internal data class OverpassBounds(
    val minlat: Double,
    val minlon: Double,
    val maxlat: Double,
    val maxlon: Double,
)
