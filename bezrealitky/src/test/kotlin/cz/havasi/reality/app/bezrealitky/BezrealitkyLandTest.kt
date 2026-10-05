package cz.havasi.reality.app.bezrealitky

import cz.havasi.reality.app.bezrealitky.api.BezrealitkyApi
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.GetRealEstatesCommand
import cz.havasi.reality.app.model.type.LandSubCategory
import cz.havasi.reality.app.model.type.ProviderType
import jakarta.ws.rs.WebApplicationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class BezrealitkyLandTest {
    private val landPage = javaClass.getResource("/fixtures/bezrealitky-land-page.html")!!.readText()
    private val command = GetRealEstatesCommand(
        type = BuildingType.LAND,
        transaction = TransactionType.SALE,
        offset = 0,
        limit = 15,
        landSubCategory = LandSubCategory.BUILDING_PLOT,
    )

    private class FakeApi(private val body: String, private val status: Int = 200) : BezrealitkyApi {
        var calls = 0
        var landType: String? = null

        override suspend fun searchEstates(
            offerType: String,
            estateType: String,
            landType: String?,
            osmValue: String,
            regionOsmIds: String,
            currency: String,
            location: String,
            page: Int,
        ): String {
            calls++
            this.landType = landType
            if (status != 200) throw WebApplicationException(status)
            return body
        }
    }

    private fun fetch(body: String, command: GetRealEstatesCommand = this.command) = runBlocking {
        val api = FakeApi(body)
        BezrealitkyRealEstatesProvider(api).getRealEstates(command) to api
    }

    @Test
    fun `land cards parse area, price and locality, per m2 suspects and cards without area are skipped`() {
        val (result, api) = fetch(landPage)

        assertEquals("STAVEBNI", api.landType)
        assertEquals(listOf("1061879-nabidka-prodej-pozemku-u-plynarny-praha", "1070001-nabidka-prodej-pozemku-praha"), result.map { it.id })

        val withStreet = result[0]
        assertEquals(217.0, withStreet.sizeInM2)
        assertEquals(8_999_999.0, withStreet.price)
        assertEquals(8_999_999.0 / 217, withStreet.pricePerM2)
        assertEquals("U Plynárny", withStreet.locality.street)
        assertEquals("Michle", withStreet.locality.district)
        assertEquals("Praha", withStreet.locality.city)
        assertEquals("BUILDING_PLOT", withStreet.subCategory)
        assertEquals(BuildingType.LAND, withStreet.mainCategory)
        assertEquals(ProviderType.BEZREALITKY, withStreet.provider)
        assertEquals(listOf("https://api.bezrealitky.cz/media/plot1.jpg"), withStreet.images)
        assertEquals("land-Praha-SALE-217", withStreet.fingerprint)
        assertEquals("Prodej pozemku U Plynárny, Praha - Michle", withStreet.name)
    }

    @Test
    fun `land card without street keeps city part as district`() {
        val withoutStreet = fetch(landPage).first[1]

        assertEquals(1024.0, withoutStreet.sizeInM2)
        assertEquals(12_500_000.0, withoutStreet.price)
        assertNull(withoutStreet.locality.street)
        assertEquals("Ďáblice", withoutStreet.locality.district)
    }

    @Test
    fun `empty page yields empty list`() {
        assertTrue(fetch("<html><body></body></html>").first.isEmpty())
        assertTrue(fetch("").first.isEmpty())
    }

    @Test
    fun `unfiltered land fetch is skipped without calling the portal`() {
        val (result, api) = fetch(landPage, command.copy(landSubCategory = null))

        assertTrue(result.isEmpty())
        assertEquals(0, api.calls)
    }

    @Test
    fun `apartment fetch sends no land type and keeps apartment parsing`() {
        val apartmentPage = """
            <article class="PropertyCard_propertyCard__moO_5">
              <h2 class="PropertyCard_propertyCardHeadline___diKI"><a href="https://www.bezrealitky.cz/nemovitosti-byty-domy/1-byt">Prodej bytu</a></h2>
              <p class="PropertyCard_propertyCardAddress__hNqyR">Vinohradská, Praha - Vinohrady</p>
              <ul class="FeaturesList_featuresList__75Wet"><li>2+kk</li><li>54 m²</li></ul>
              <span class="PropertyPrice_propertyPriceAmount__WdEE1">8 100 000 Kč</span>
            </article>
        """.trimIndent()
        val (result, api) = fetch(apartmentPage, command.copy(type = BuildingType.APARTMENT, landSubCategory = null))

        assertNull(api.landType)
        assertEquals(54.0, result.single().sizeInM2)
        assertEquals("2+kk", result.single().subCategory)
        assertEquals("Vinohrady", result.single().locality.district)
    }

    @Test
    fun `redirect past the last page yields an empty list`() {
        val result = runBlocking { BezrealitkyRealEstatesProvider(FakeApi(landPage, status = 307)).getRealEstates(command) }

        assertTrue(result.isEmpty())
    }
}
