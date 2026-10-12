package cz.havasi.reality.app.service

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.ApartmentDuplicate
import cz.havasi.reality.app.model.Coordinates
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.type.ProviderType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class AreaPriceServiceTest {
    private val repository = FakeApartmentRepository()
    private val service = AreaPriceService(repository)
    private val center = Coordinates(50.0755, 14.4378)

    private fun listing(id: String, metersNorth: Int, pricePerM2: Double, size: Double = 60.0): Apartment {
        val apartment = apartment(id, ProviderType.SREALITY, size, pricePerM2 * size)
        val latitude = center.latitude + metersNorth / METERS_PER_DEGREE_OF_LATITUDE
        return apartment.copy(locality = apartment.locality.copy(latitude = latitude, longitude = center.longitude))
    }

    private suspend fun neighbours(count: Int, metersNorth: Int, pricePerM2: Double, size: Double = 60.0, prefix: String = "n$metersNorth") =
        repeat(count) { repository.save(listing("$prefix-$it", metersNorth, pricePerM2, size)) }

    @Test
    fun `cheaper than the area comes out as a negative difference`() = runTest {
        val subject = listing("subject", 0, 117_500.0)
        repository.save(subject)
        neighbours(10, 200, 125_000.0)

        val comparison = service.compare(subject)!!

        assertEquals(-6, comparison.differencePercent)
        assertEquals(125_000.0, comparison.areaPricePerM2)
        assertEquals(117_500.0, comparison.pricePerM2)
        assertEquals(10, comparison.comparableCount)
        assertEquals(500, comparison.radiusInMeters)
    }

    @Test
    fun `a few mispriced neighbours don't move the area price`() = runTest {
        neighbours(9, 200, 100_000.0)
        neighbours(2, 300, 1_000_000.0)

        val comparison = service.compare(listing("subject", 0, 110_000.0))!!

        assertEquals(100_000.0, comparison.areaPricePerM2)
        assertEquals(10, comparison.differencePercent)
    }

    @Test
    fun `radius widens until there are enough similar listings`() = runTest {
        neighbours(5, 300, 100_000.0)
        neighbours(6, 800, 100_000.0)
        neighbours(3, 1_500, 100_000.0)

        val comparison = service.compare(listing("subject", 0, 100_000.0))!!

        assertEquals(1_000, comparison.radiusInMeters)
        assertEquals(11, comparison.comparableCount)
        assertEquals(0, comparison.differencePercent)
    }

    @Test
    fun `no comparison without enough similar listings within 2 km`() = runTest {
        neighbours(9, 300, 100_000.0)
        neighbours(5, 2_500, 100_000.0)
        neighbours(5, 300, 100_000.0, size = 120.0, prefix = "large")

        assertNull(service.compare(listing("subject", 0, 100_000.0)))
    }

    @Test
    fun `listing on several portals is compared at its lowest price`() = runTest {
        neighbours(10, 200, 125_000.0)
        val subject = listing("subject", 0, 125_000.0).let {
            it.copy(duplicates = listOf(ApartmentDuplicate(url = "https://example.com/dup", price = 117_500.0 * 60, pricePerM2 = 117_500.0)))
        }

        assertEquals(-6, service.compare(subject)!!.differencePercent)
    }

    @Test
    fun `rent listings are compared with rent listings only`() = runTest {
        neighbours(10, 200, 125_000.0)
        val rent = listing("subject", 0, 400.0).copy(transactionType = TransactionType.RENT)

        assertNull(service.compare(rent))
    }

    @Test
    fun `placeholder prices and listings without coordinates are not compared`() = runTest {
        neighbours(10, 200, 125_000.0)
        val subject = listing("subject", 0, 125_000.0)

        assertNull(service.compare(subject.copy(pricePerM2 = 1.0)))
        assertNull(service.compare(subject.copy(pricePerM2 = null)))
        assertNull(service.compare(subject.copy(locality = subject.locality.copy(latitude = null, longitude = null))))
    }
}

private const val METERS_PER_DEGREE_OF_LATITUDE = 111_195.0
