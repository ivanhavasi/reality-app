package cz.havasi.reality.app.sreality

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.CurrencyType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.GetRealEstatesCommand
import cz.havasi.reality.app.model.type.LandSubCategory
import cz.havasi.reality.app.model.type.ProviderType
import cz.havasi.reality.app.sreality.api.SrealityApi
import cz.havasi.reality.app.sreality.model.SrealitySearchResult
import kotlinx.coroutines.runBlocking
import org.jboss.resteasy.reactive.RestResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class SrealityLandMapperTest {
    private val baseUrl = "https://www.sreality.cz"
    private val command = GetRealEstatesCommand(
        type = BuildingType.LAND,
        transaction = TransactionType.SALE,
        offset = 0,
        limit = 22,
        landSubCategory = LandSubCategory.BUILDING_PLOT,
    )
    private val searchResult: SrealitySearchResult = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .readValue(javaClass.getResource("/fixtures/sreality-land-search.json")!!)

    private fun estate(hashId: String) = searchResult.results.first { it.hashId == hashId }

    @Test
    fun `per m2 plot uses price summary as total and area from name`() {
        val land = estate("1140731980").toLand(command, baseUrl)!!

        assertEquals(12_594_710.0, land.price)
        assertEquals(6329.0, land.sizeInM2)
        assertEquals(1990.0, land.pricePerM2)
        assertEquals("BUILDING_PLOT", land.subCategory)
        assertEquals(BuildingType.LAND, land.mainCategory)
        assertEquals(CurrencyType.CZK, land.currency)
        assertEquals(ProviderType.SREALITY, land.provider)
        assertEquals("Radotín", land.locality.district)
        assertNull(land.locality.street)
        assertEquals("land-Praha-SALE-6329", land.fingerprint)
    }

    @Test
    fun `price on request plot has zero price and zero price per m2`() {
        val land = estate("2222").toLand(command, baseUrl)!!

        assertEquals(0.0, land.price)
        assertEquals(0.0, land.pricePerM2)
        assertEquals(850.0, land.sizeInM2)
        assertEquals("Spořická", land.locality.street)
    }

    @Test
    fun `per m2 price above plausible limit is treated as total`() {
        val land = estate("3333").toLand(command, baseUrl)!!

        assertEquals(3_500_000.0, land.price)
        assertEquals(3500.0, land.pricePerM2)
    }

    @Test
    fun `land url points to pozemek detail`() {
        assertEquals(
            "https://www.sreality.cz/detail/prodej/pozemek/bydleni/praha-radotin-/1140731980",
            estate("1140731980").toLand(command, baseUrl)!!.url,
        )
        assertEquals(
            "https://www.sreality.cz/detail/prodej/pozemek/bydleni/praha-dolni-chabry-sporicka/2222",
            estate("2222").toLand(command, baseUrl)!!.url,
        )
    }

    @Test
    fun `land without area in name is skipped`() {
        assertNull(estate("4444").toLand(command, baseUrl))
    }

    @Test
    fun `category sub is sent only for land`() {
        assertEquals("19", command.resolveCategorySub())
        assertNull(command.copy(type = BuildingType.APARTMENT).resolveCategorySub())
        assertNull(command.copy(landSubCategory = null).resolveCategorySub())
    }

    @Test
    fun `one broken estate does not drop the page`() = runBlocking {
        var sentCategorySub: String? = null
        val api = object : SrealityApi {
            override suspend fun searchEstates(
                categoryType: Int,
                categoryMain: Int,
                categorySub: String?,
                localityCountryId: Int,
                localityRegionId: Int,
                limit: Int,
                offset: Int,
                lang: String,
                sort: String,
                topTimestampTo: Long,
            ): RestResponse<SrealitySearchResult> {
                sentCategorySub = categorySub
                return RestResponse.ok(searchResult)
            }
        }

        val result = SrealityRealEstatesProvider(api, baseUrl).getRealEstates(command)

        assertEquals(listOf("1140731980", "2222", "3333"), result.map { it.id })
        assertEquals("19", sentCategorySub)
    }
}
