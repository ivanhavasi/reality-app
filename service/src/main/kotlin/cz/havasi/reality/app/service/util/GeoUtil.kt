package cz.havasi.reality.app.service.util

import cz.havasi.reality.app.model.Coordinates
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_IN_METERS = 6_371_000.0

internal fun Coordinates.distanceInMetersTo(other: Coordinates): Double {
    val latitudeDelta = Math.toRadians(other.latitude - latitude)
    val longitudeDelta = Math.toRadians(other.longitude - longitude)
    val haversine = sin(latitudeDelta / 2).pow(2) +
        cos(Math.toRadians(latitude)) * cos(Math.toRadians(other.latitude)) * sin(longitudeDelta / 2).pow(2)
    return 2 * EARTH_RADIUS_IN_METERS * asin(sqrt(haversine))
}

// south-west and north-east corners of the square around the point; its corners are farther than the radius
internal fun Coordinates.boundingBox(radiusInMeters: Int): Pair<Coordinates, Coordinates> {
    val latitudeDelta = Math.toDegrees(radiusInMeters / EARTH_RADIUS_IN_METERS)
    val longitudeDelta = Math.toDegrees(radiusInMeters / (EARTH_RADIUS_IN_METERS * cos(Math.toRadians(latitude))))
    return Coordinates(latitude - latitudeDelta, longitude - longitudeDelta) to
        Coordinates(latitude + latitudeDelta, longitude + longitudeDelta)
}

// shoelace on a local flat projection, plenty for a park; the ring may or may not repeat its first point
internal fun List<Coordinates>.areaInSquareMeters(): Double {
    if (size < 3) return 0.0
    val latitudeScale = Math.toRadians(1.0) * EARTH_RADIUS_IN_METERS
    val longitudeScale = latitudeScale * cos(Math.toRadians(map { it.latitude }.average()))
    val points = map { it.longitude * longitudeScale to it.latitude * latitudeScale }
    val doubled = points.indices.sumOf { i ->
        val (x1, y1) = points[i]
        val (x2, y2) = points[(i + 1) % points.size]
        x1 * y2 - x2 * y1
    }
    return abs(doubled) / 2
}

// adds points along long edges, so that the nearest point of the line is never far from the nearest vertex
internal fun List<Coordinates>.densified(maxStepInMeters: Double): List<Coordinates> {
    if (size < 2) return this
    return zipWithNext().flatMap { (from, to) ->
        val steps = ceil(from.distanceInMetersTo(to) / maxStepInMeters).toInt().coerceAtLeast(1)
        (0 until steps).map { step ->
            val fraction = step.toDouble() / steps
            Coordinates(
                from.latitude + (to.latitude - from.latitude) * fraction,
                from.longitude + (to.longitude - from.longitude) * fraction,
            )
        }
    } + last()
}
