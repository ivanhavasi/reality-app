package cz.havasi.reality.app.bezrealitky

import cz.havasi.reality.app.bezrealitky.api.BezrealitkyApi
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.GetRealEstatesCommand
import cz.havasi.reality.app.model.type.ProviderType
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class BezrealitkyHouseTest {
    private val housePage = javaClass.getResource("/fixtures/bezrealitky-house-page.html")!!.readText()
    private val command = GetRealEstatesCommand(
        type = BuildingType.HOUSE,
        transaction = TransactionType.SALE,
        offset = 0,
        limit = 22,
    )

    private class FakeApi(private val body: String) : BezrealitkyApi {
        var estateType: String? = null
        var landType: String? = "not called"

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
            this.estateType = estateType
            this.landType = landType
            return body
        }
    }

    private val fetched by lazy {
        runBlocking {
            val api = FakeApi(housePage)
            BezrealitkyRealEstatesProvider(api).getRealEstates(command) to api
        }
    }
    private val result get() = fetched.first

    @Test
    fun `house fetch asks for DUM without land type and skips cards without area`() {
        assertEquals("DUM", fetched.second.estateType)
        assertNull(fetched.second.landType)
        assertEquals(
            listOf(
                "1066235-nabidka-prodej-domu-k-odpocinku-praha",
                "1062332-nabidka-prodej-domu-nad-udolim-praha",
                "1070002-nabidka-prodej-domu-praha",
            ),
            result.map { it.id },
        )
    }

    @Test
    fun `house card parses usable area, plot area, price and locality`() {
        val house = result[0]

        assertEquals(156.0, house.sizeInM2)
        assertEquals(296.0, house.landSizeInM2)
        assertEquals(18_900_000.0, house.price)
        assertEquals(18_900_000.0 / 156, house.pricePerM2)
        assertEquals("K Odpočinku", house.locality.street)
        assertEquals("Horní Počernice", house.locality.district)
        assertEquals("Praha", house.locality.city)
        assertNull(house.subCategory)
        assertEquals(BuildingType.HOUSE, house.mainCategory)
        assertEquals(ProviderType.BEZREALITKY, house.provider)
        assertEquals(listOf("https://api.bezrealitky.cz/media/house1.jpg"), house.images)
        assertEquals("house-Praha-SALE-156", house.fingerprint)
        assertEquals("Prodej domu K Odpočinku, Praha - Horní Počernice", house.name)
    }

    @Test
    fun `plot area with thousands separator`() {
        assertEquals(427.0, result[1].sizeInM2)
        assertEquals(1676.0, result[1].landSizeInM2)
    }

    @Test
    fun `house without plot area keeps the usable area`() {
        val house = result[2]

        assertEquals(170.0, house.sizeInM2)
        assertNull(house.landSizeInM2)
        assertNull(house.locality.street)
        assertEquals("Řepy", house.locality.district)
    }
}
