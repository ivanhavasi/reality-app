package cz.havasi.reality.app.bezrealitky

import cz.havasi.reality.app.bezrealitky.api.BezrealitkyApi
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.GetRealEstatesCommand
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class BezrealitkyGpsTest {
    private val apartmentPage = javaClass.getResource("/fixtures/bezrealitky-apartment-page.html")!!.readText()
    private val command = GetRealEstatesCommand(
        type = BuildingType.APARTMENT,
        transaction = TransactionType.SALE,
        offset = 0,
        limit = 22,
    )

    private class FakeApi(private val body: String) : BezrealitkyApi {
        override suspend fun searchEstates(
            offerType: String,
            estateType: String,
            landType: String?,
            osmValue: String,
            regionOsmIds: String,
            currency: String,
            location: String,
            page: Int,
        ): String = body
    }

    private fun fetch(body: String) = runBlocking { BezrealitkyRealEstatesProvider(FakeApi(body)).getRealEstates(command) }

    @Test
    fun `apartment gets its coordinates from the embedded advert data`() {
        val apartment = fetch(apartmentPage)[0]

        assertEquals("1077346-nabidka-prodej-bytu-krenova-praha", apartment.id)
        assertEquals(50.0886487, apartment.locality.latitude)
        assertEquals(14.3449007, apartment.locality.longitude)
        assertEquals("Břevnov", apartment.locality.district)
    }

    @Test
    fun `advert without gps stays without coordinates`() {
        val apartment = fetch(apartmentPage)[1]

        assertEquals("1070329-nabidka-prodej-bytu-sladkovicova-praha", apartment.id)
        assertNull(apartment.locality.latitude)
        assertNull(apartment.locality.longitude)
    }

    @Test
    fun `broken embedded data keeps the listings, only without coordinates`() {
        val broken = apartmentPage.replace("\"apolloCache\":{", "\"apolloCache\":{{")

        val result = fetch(broken)

        assertEquals(2, result.size)
        assertNull(result[0].locality.latitude)
    }
}
