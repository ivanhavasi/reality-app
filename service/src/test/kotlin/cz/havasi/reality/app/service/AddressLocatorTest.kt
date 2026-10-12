package cz.havasi.reality.app.service

import cz.havasi.reality.app.model.Coordinates
import cz.havasi.reality.app.model.Locality
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class AddressLocatorTest {
    private val vinohradska = listOf(
        segment(50.0770, 14.4884, "Vinohradská, Třebešín, Praha 10, obvod Praha 10, Praha, 100 00, Česko"),
        segment(50.0778, 14.4610, "Vinohradská, Žižkov, obvod Praha 3, Praha, 100 83, Česko"),
        segment(50.0770, 14.4438, "Vinohradská, Vyšehrad, obvod Praha 2, Praha, 120 09, Česko"),
    )
    private val geocoder = FakeGeocodingClient(mapOf("Vinohradská" to vinohradska))
    private val cache = FakeGeocodingCache()
    private val locator = AddressLocator(geocoder, cache)

    private fun locality(street: String?, district: String? = null) =
        Locality(city = "Praha", district = district, street = street, streetNumber = null, latitude = null, longitude = null)

    @Test
    fun `picks the piece of a long street in the listing's city part`() = runTest {
        assertEquals(Coordinates(50.0778, 14.4610), locator.locate(locality("Vinohradská", "Žižkov")))
    }

    @Test
    fun `street whose pieces don't name the city part is placed in its middle`() = runTest {
        val located = locator.locate(locality("Vinohradská", "Vinohrady"))!!

        assertEquals(vinohradska.map { it.coordinates.latitude }.average(), located.latitude)
        assertEquals(vinohradska.map { it.coordinates.longitude }.average(), located.longitude)
    }

    @Test
    fun `each street is looked up once, unknown ones included`() = runTest {
        locator.locate(locality("Vinohradská", "Žižkov"))
        locator.locate(locality("vinohradská", "Třebešín"))
        assertNull(locator.locate(locality("Neexistující")))
        assertNull(locator.locate(locality("Neexistující")))

        assertEquals(listOf("Vinohradská", "Neexistující"), geocoder.asked)
    }

    @Test
    fun `failed lookup isn't cached, so the street is tried again`() = runTest {
        geocoder.failing = true
        assertNull(locator.locate(locality("Vinohradská")))
        geocoder.failing = false

        assertEquals(Coordinates(50.0770, 14.4884), locator.locate(locality("Vinohradská", "Třebešín")))
        assertEquals(2, geocoder.asked.size)
    }

    @Test
    fun `values that aren't street names are never looked up`() = runTest {
        assertNull(locator.locate(locality("Praha 4 - Krč")))
        assertNull(locator.locate(locality("Unknown street")))
        assertNull(locator.locate(locality(null, "Krč")))

        assertTrue(geocoder.asked.isEmpty())
    }

    @Test
    fun `lookups are at least a second apart`() = runTest {
        locator.locate(locality("Vinohradská"))
        locator.locate(locality("Korunní"))
        locator.locate(locality("Slezská"))

        assertTrue(testScheduler.currentTime >= 3 * 1_000)
    }
}
