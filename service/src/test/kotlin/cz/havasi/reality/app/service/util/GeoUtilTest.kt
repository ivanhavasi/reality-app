package cz.havasi.reality.app.service.util

import cz.havasi.reality.app.model.Coordinates
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class GeoUtilTest {
    private val prague = Coordinates(50.0, 14.0)

    @Test
    fun `distance along a meridian and along a parallel`() {
        assertEquals(1_112.0, prague.distanceInMetersTo(Coordinates(50.01, 14.0)), 1.0)
        // a degree of longitude shrinks with the cosine of the latitude
        assertEquals(714.7, prague.distanceInMetersTo(Coordinates(50.0, 14.01)), 1.0)
        assertEquals(0.0, prague.distanceInMetersTo(prague))
    }

    @Test
    fun `bounding box edges are the radius away from the center`() {
        val (southWest, northEast) = prague.boundingBox(1_000)

        assertEquals(1_000.0, prague.distanceInMetersTo(Coordinates(northEast.latitude, prague.longitude)), 0.5)
        assertEquals(1_000.0, prague.distanceInMetersTo(Coordinates(southWest.latitude, prague.longitude)), 0.5)
        assertEquals(1_000.0, prague.distanceInMetersTo(Coordinates(prague.latitude, northEast.longitude)), 0.5)
        assertEquals(1_000.0, prague.distanceInMetersTo(Coordinates(prague.latitude, southWest.longitude)), 0.5)
    }

    @Test
    fun `area of a 100 m square`() {
        val north = 100 / 111_195.0
        val east = 100 / (111_195.0 * Math.cos(Math.toRadians(50.0)))
        val square = listOf(prague, Coordinates(50.0, 14.0 + east), Coordinates(50.0 + north, 14.0 + east), Coordinates(50.0 + north, 14.0))

        assertEquals(10_000.0, square.areaInSquareMeters(), 20.0)
        assertEquals(10_000.0, (square + prague).areaInSquareMeters(), 20.0)
    }

    @Test
    fun `densified line keeps its ends and has no step longer than asked`() {
        val line = listOf(prague, Coordinates(50.01, 14.0))

        val densified = line.densified(100.0)

        assertEquals(13, densified.size) // 1112 m in 12 steps
        assertEquals(prague, densified.first())
        assertEquals(Coordinates(50.01, 14.0), densified.last())
        assertTrue(densified.zipWithNext().all { (from, to) -> from.distanceInMetersTo(to) <= 100.0 })
    }
}
