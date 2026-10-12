package cz.havasi.reality.app.rest.client

import cz.havasi.reality.app.model.Coordinates
import cz.havasi.reality.app.rest.client.api.NominatimApi
import cz.havasi.reality.app.rest.client.model.NominatimPlace
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class RestNominatimClientTest {
    private val requests = mutableListOf<Map<String, Any>>()
    private val client = RestNominatimClient(
        object : NominatimApi {
            override suspend fun searchStreet(
                street: String,
                city: String,
                country: String,
                layer: String,
                limit: Int,
                format: String,
                acceptLanguage: String,
            ): List<NominatimPlace> {
                requests += mapOf("street" to street, "city" to city, "country" to country, "layer" to layer, "limit" to limit)
                return listOf(
                    NominatimPlace("50.0450543", "14.4492123", "Budějovická, Zelená Liška, Krč, Praha 4, Praha, 140 02, Česko"),
                    NominatimPlace("not a number", "14.45", "broken"),
                )
            }
        },
    )

    @Test
    fun `asks for the street itself and reads the coordinates of every piece`() = runTest {
        val segments = client.findStreet("Budějovická", "Praha")

        assertEquals(
            mapOf("street" to "Budějovická", "city" to "Praha", "country" to "Česko", "layer" to "address", "limit" to 10),
            requests.single(),
        )
        assertEquals(listOf(Coordinates(50.0450543, 14.4492123)), segments.map { it.coordinates })
        assertEquals("Budějovická, Zelená Liška, Krč, Praha 4, Praha, 140 02, Česko", segments.single().description)
    }
}
