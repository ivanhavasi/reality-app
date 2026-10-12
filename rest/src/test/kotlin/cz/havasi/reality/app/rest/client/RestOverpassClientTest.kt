package cz.havasi.reality.app.rest.client

import cz.havasi.reality.app.model.Coordinates
import cz.havasi.reality.app.model.Place
import cz.havasi.reality.app.model.PlaceCategory
import cz.havasi.reality.app.rest.client.api.OverpassApi
import cz.havasi.reality.app.rest.client.model.OverpassBounds
import cz.havasi.reality.app.rest.client.model.OverpassElement
import cz.havasi.reality.app.rest.client.model.OverpassMember
import cz.havasi.reality.app.rest.client.model.OverpassPoint
import cz.havasi.reality.app.rest.client.model.OverpassResponse
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class RestOverpassClientTest {
    private val queries = mutableListOf<String>()
    private var response = OverpassResponse()
    private val client = RestOverpassClient(
        object : OverpassApi {
            override suspend fun query(query: String): OverpassResponse {
                queries += query
                return response
            }
        },
    )
    private val southWest = Coordinates(49.91, 14.18)
    private val northEast = Coordinates(50.21, 14.75)

    private fun p(lat: Double, lon: Double) = OverpassPoint(lat, lon)
    private fun c(lat: Double, lon: Double) = Coordinates(lat, lon)

    @Test
    fun `points come from nodes and from the centers of buildings`() = runTest {
        response = OverpassResponse(
            listOf(
                OverpassElement(type = "node", id = 1, lat = 50.07, lon = 14.43, tags = mapOf("name" to "Albert")),
                OverpassElement(type = "way", id = 2, center = p(50.08, 14.44), tags = mapOf("name" to " ")),
                OverpassElement(type = "relation", id = 3),
            ),
        )

        val places = client.findPlaces(PlaceCategory.SUPERMARKET, southWest, northEast)

        assertEquals("""[out:json][timeout:90];nwr["shop"="supermarket"](49.91,14.18,50.21,14.75);out center;""", queries.single())
        assertEquals(
            listOf(
                Place("node/1", PlaceCategory.SUPERMARKET, "Albert", c(50.07, 14.43)),
                Place("way/2", PlaceCategory.SUPERMARKET, null, c(50.08, 14.44)),
            ),
            places,
        )
    }

    @Test
    fun `park outlines are read and a boundary split into ways is joined`() = runTest {
        val bounds = OverpassBounds(50.0, 14.0, 50.02, 14.02)
        response = OverpassResponse(
            listOf(
                OverpassElement(
                    type = "way", id = 10, bounds = bounds,
                    geometry = listOf(p(50.0, 14.0), p(50.0, 14.01), p(50.01, 14.01), p(50.0, 14.0)),
                ),
                OverpassElement(
                    type = "relation", id = 11, bounds = bounds, tags = mapOf("name" to "Stromovka"),
                    members = listOf(
                        OverpassMember("way", "outer", listOf(p(50.0, 14.0), p(50.0, 14.02))),
                        // stored the other way round
                        OverpassMember("way", "outer", listOf(p(50.0, 14.0), p(50.02, 14.0), p(50.02, 14.02))),
                        OverpassMember("way", "outer", listOf(p(50.02, 14.02), p(50.0, 14.02))),
                        OverpassMember("way", "inner", listOf(p(50.01, 14.01), p(50.01, 14.011), p(50.011, 14.01), p(50.01, 14.01))),
                        OverpassMember("node", "label", null),
                    ),
                ),
                OverpassElement(type = "node", id = 12, lat = 50.0, lon = 14.0),
            ),
        )

        val parks = client.findPlaces(PlaceCategory.PARK, southWest, northEast)

        assert(queries.single().endsWith("""nwr["leisure"="park"](49.91,14.18,50.21,14.75);out geom;"""))
        assertEquals(listOf("way/10", "relation/11"), parks.map { it.id })
        assertEquals(50.01, parks[0].center.latitude, 1e-9)
        assertEquals(14.01, parks[0].center.longitude, 1e-9)
        val stromovka = parks[1]
        assertEquals("Stromovka", stromovka.name)
        assertEquals(
            listOf(listOf(c(50.0, 14.0), c(50.0, 14.02), c(50.02, 14.02), c(50.02, 14.0), c(50.0, 14.0))),
            stromovka.outerRings,
        )
        assertEquals(1, stromovka.innerRings.size)
    }

    @Test
    fun `ring left open where the boundary leaves the queried box`() {
        val rings = joinRings(listOf(listOf(c(50.0, 14.0), c(50.0, 14.1)), listOf(c(50.1, 14.1), c(50.1, 14.0))))

        assertEquals(2, rings.size)
    }
}
