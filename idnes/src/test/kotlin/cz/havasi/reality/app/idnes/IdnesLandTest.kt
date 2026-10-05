package cz.havasi.reality.app.idnes

import cz.havasi.reality.app.idnes.api.IdnesApi
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.GetRealEstatesCommand
import cz.havasi.reality.app.model.type.LandSubCategory
import cz.havasi.reality.app.model.type.ProviderType
import jakarta.ws.rs.WebApplicationException
import kotlinx.coroutines.runBlocking
import org.jboss.resteasy.reactive.RestResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class IdnesLandTest {
    private val landPage = javaClass.getResource("/fixtures/idnes-land-page.html")!!.readText()
    private val command = GetRealEstatesCommand(
        type = BuildingType.LAND,
        transaction = TransactionType.SALE,
        offset = 0,
        limit = 22,
        landSubCategory = LandSubCategory.BUILDING_PLOT,
    )

    private class FakeApi(private val respond: () -> RestResponse<String>) : IdnesApi {
        var calls = 0
        var subType: String? = null

        override suspend fun searchEstatesForPageZero(
            transactionType: String,
            buildingType: String,
            location: String,
            page: Int?,
        ): RestResponse<String> = respond().also { calls++ }

        override suspend fun searchEstatesWithSubType(
            transactionType: String,
            buildingType: String,
            subType: String,
            location: String,
            page: Int?,
        ): RestResponse<String> {
            calls++
            this.subType = subType
            return respond()
        }
    }

    private fun fetch(
        command: GetRealEstatesCommand = this.command,
        respond: () -> RestResponse<String> = { RestResponse.ok(landPage) },
    ) = runBlocking {
        val api = FakeApi(respond)
        IdnesRealEstateProvider(api).getRealEstates(command) to api
    }

    private val result by lazy { fetch().first.associateBy { it.id } }

    @Test
    fun `land fetch uses sub type path and skips per m2 suspects and cards without area`() {
        val (apartments, api) = fetch()

        assertEquals("stavebni-pozemek", api.subType)
        assertEquals(
            listOf(
                "6a1070ded3d20e1db406c2c5",
                "6a1070ded3d20e1db406c2c6",
                "6a1070ded3d20e1db406c2c7",
                "69e77362842fad586b02a7da",
            ),
            apartments.map { it.id },
        )
    }

    @Test
    fun `building plot card parses area, price, subtype and locality`() {
        val land = result.getValue("6a1070ded3d20e1db406c2c5")

        assertEquals("Prodej stavebního pozemku 473 m²", land.name)
        assertEquals(473.0, land.sizeInM2)
        assertEquals(11_500_000.0, land.price)
        assertEquals("BUILDING_PLOT", land.subCategory)
        assertEquals("Spořická", land.locality.street)
        assertEquals("Dolní Chabry", land.locality.district)
        assertEquals(BuildingType.LAND, land.mainCategory)
        assertEquals(ProviderType.IDNES, land.provider)
        assertEquals(listOf("https://sta-reality2.1gr.cz/sta/compile/thumbs/84c83db6ca93c077671548ed0e1be.webp"), land.images)
        assertEquals("land-Praha-SALE-473", land.fingerprint)
    }

    @Test
    fun `field with thousands separator and price on request`() {
        val land = result.getValue("6a1070ded3d20e1db406c2c6")

        assertEquals(24_066.0, land.sizeInM2)
        assertEquals(0.0, land.price)
        assertEquals(0.0, land.pricePerM2)
        assertEquals("FIELD", land.subCategory)
        assertNull(land.locality.street)
        assertEquals("Satalice", land.locality.district)
        assertTrue(land.images.isEmpty())
    }

    @Test
    fun `garden with one word subtype`() {
        val land = result.getValue("6a1070ded3d20e1db406c2c7")

        assertEquals(295.0, land.sizeInM2)
        assertEquals("GARDEN", land.subCategory)
        assertEquals("Radotín", land.locality.district)
    }

    @Test
    fun `decimal comma price parses`() {
        val land = result.getValue("69e77362842fad586b02a7da")

        assertEquals(17_999_999.44, land.price)
        assertEquals(1418.0, land.sizeInM2)
        assertNull(land.locality.district)
    }

    @Test
    fun `size regex does not glue disposition to area`() {
        assertEquals(65.0, "Prodej bytu 2+1 65 m²".parseSize())
        assertEquals(24_066.0, "Prodej pole 24 066 m²".parseSize())
        assertEquals(295.0, "Prodej zahrady 295 m²".parseSize())
        assertNull("Prodej stavebního pozemku".parseSize())
    }

    @Test
    fun `title noun resolves land subtype`() {
        assertEquals(LandSubCategory.BUILDING_PLOT, "Prodej stavebního pozemku 1 418 m²".parseLandSubCategory())
        assertEquals(LandSubCategory.FIELD, "Prodej pole 24 066 m²".parseLandSubCategory())
        assertEquals(LandSubCategory.OTHER, "Prodej pozemku 500 m²".parseLandSubCategory())
        assertNull("Prodej bytu 2+1 65 m²".parseLandSubCategory())
    }

    @Test
    fun `apartment card keeps its parsing and gets decimal comma price`() {
        val apartmentPage = """
            <div class="c-products__inner">
              <a href="https://reality.idnes.cz/detail/prodej/byt/praha-2/abc123/" class="c-products__link">
                <span class="c-products__img"><img data-src="https://img/1.webp"></span>
                <h2 class="c-products__title"><span class="text-capitalize">prodej</span> bytu 2+1 65&nbsp;m²</h2>
                <p class="c-products__info">Mánesova, Praha 2 - Vinohrady</p>
                <p class="c-products__price"><strong>17&nbsp;999&nbsp;999,44 Kč</strong></p>
              </a>
            </div>
        """.trimIndent()
        val (apartments, api) = fetch(command.copy(type = BuildingType.APARTMENT, landSubCategory = null)) {
            RestResponse.ok(apartmentPage)
        }

        val apartment = apartments.single()
        assertNull(api.subType)
        assertEquals(65.0, apartment.sizeInM2)
        assertEquals("2+1", apartment.subCategory)
        assertEquals(17_999_999.44, apartment.price)
        assertEquals(BuildingType.APARTMENT, apartment.mainCategory)
    }

    @Test
    fun `404 past the last page yields empty list`() {
        val (apartments, api) = fetch { throw WebApplicationException(404) }

        assertTrue(apartments.isEmpty())
        assertEquals(1, api.calls)
    }

    @Test
    fun `redirect past the last page yields empty list`() {
        assertTrue(fetch { RestResponse.status(RestResponse.Status.FOUND) }.first.isEmpty())
    }

    @Test
    fun `unsupported land subtype is not fetched`() {
        val (apartments, api) = fetch(command.copy(landSubCategory = LandSubCategory.ORCHARD_VINEYARD))

        assertTrue(apartments.isEmpty())
        assertEquals(0, api.calls)
    }
}
