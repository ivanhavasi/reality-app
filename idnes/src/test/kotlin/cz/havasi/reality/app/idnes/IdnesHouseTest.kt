package cz.havasi.reality.app.idnes

import cz.havasi.reality.app.idnes.api.IdnesApi
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.GetRealEstatesCommand
import cz.havasi.reality.app.model.type.HouseSubCategory
import cz.havasi.reality.app.model.type.ProviderType
import kotlinx.coroutines.runBlocking
import org.jboss.resteasy.reactive.RestResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class IdnesHouseTest {
    private val housePage = javaClass.getResource("/fixtures/idnes-house-page.html")!!.readText()
    private val command = GetRealEstatesCommand(
        type = BuildingType.HOUSE,
        transaction = TransactionType.SALE,
        offset = 0,
        limit = 22,
    )

    private class FakeApi(private val body: String) : IdnesApi {
        var buildingType: String? = null
        var subType: String? = null

        override suspend fun searchEstatesForPageZero(
            transactionType: String,
            buildingType: String,
            location: String,
            page: Int?,
        ): RestResponse<String> {
            this.buildingType = buildingType
            return RestResponse.ok(body)
        }

        override suspend fun searchEstatesWithSubType(
            transactionType: String,
            buildingType: String,
            subType: String,
            location: String,
            page: Int?,
        ): RestResponse<String> {
            this.subType = subType
            return RestResponse.ok(body)
        }
    }

    private val fetched by lazy {
        runBlocking {
            val api = FakeApi(housePage)
            IdnesRealEstateProvider(api).getRealEstates(command) to api
        }
    }
    private val result by lazy { fetched.first.associateBy { it.id } }

    @Test
    fun `house fetch uses the domy path and skips cards without area`() {
        assertEquals("domy", fetched.second.buildingType)
        assertNull(fetched.second.subType)
        assertEquals(
            listOf(
                "6ab4ee899f4c9e9d6e0303f0",
                "6accbf0bb5384ad3c00d58d5",
                "6a180001ba074ecd9e05d2b0",
                "6ac4106ede7f7dddb304fb33",
                "6a99205deabfaae64409ec77",
            ),
            fetched.first.map { it.id },
        )
    }

    @Test
    fun `house card parses usable area, plot area, price and locality`() {
        val house = result.getValue("6ab4ee899f4c9e9d6e0303f0")

        assertEquals("Prodej domu 342 m² s pozemkem 769 m²", house.name.replace(' ', ' '))
        assertEquals(342.0, house.sizeInM2)
        assertEquals(769.0, house.landSizeInM2)
        assertEquals(23_890_000.0, house.price)
        assertEquals(23_890_000.0 / 342, house.pricePerM2)
        assertEquals("Armádní", house.locality.street)
        assertEquals("Suchdol", house.locality.district)
        assertNull(house.subCategory)
        assertEquals(BuildingType.HOUSE, house.mainCategory)
        assertEquals(ProviderType.IDNES, house.provider)
        assertEquals(listOf("https://sta-reality2.1gr.cz/sta/compile/thumbs/1/b/c/0448fe97382b960e9ea53b317d0a3.webp"), house.images)
        assertEquals("house-Praha-SALE-342", house.fingerprint)
    }

    @Test
    fun `price on request and plot area with thousands separator`() {
        val house = result.getValue("6accbf0bb5384ad3c00d58d5")

        assertEquals(252.0, house.sizeInM2)
        assertEquals(1774.0, house.landSizeInM2)
        assertEquals(0.0, house.price)
        assertEquals(0.0, house.pricePerM2)
        assertEquals("Radotín", house.locality.district)
    }

    @Test
    fun `placeholder plot of 1 m2 is dropped`() {
        val house = result.getValue("6a180001ba074ecd9e05d2b0")

        assertEquals(161.0, house.sizeInM2)
        assertNull(house.landSizeInM2)
        assertNull(house.locality.street)
        assertNull(house.locality.district)
    }

    @Test
    fun `cottage and villa without plot get their subtype`() {
        val cottage = result.getValue("6ac4106ede7f7dddb304fb33")
        val villa = result.getValue("6a99205deabfaae64409ec77")

        assertEquals(21.0, cottage.sizeInM2)
        assertNull(cottage.landSizeInM2)
        assertEquals("COTTAGE", cottage.subCategory)
        assertEquals("Lipence", cottage.locality.district)
        assertEquals(474.0, villa.sizeInM2)
        assertEquals("VILLA", villa.subCategory)
        assertEquals(75_000_000.0, villa.price)
    }

    @Test
    fun `title noun resolves house subtype`() {
        assertEquals(HouseSubCategory.VILLA, "Prodej vily 474 m²".parseHouseSubCategory())
        assertEquals(HouseSubCategory.COTTAGE, "Prodej chaty/chalupy 21 m²".parseHouseSubCategory())
        assertNull("Prodej domu 342 m² s pozemkem 769 m²".parseHouseSubCategory())
        assertNull("Pronájem domu 120 m²".parseHouseSubCategory())
    }
}
