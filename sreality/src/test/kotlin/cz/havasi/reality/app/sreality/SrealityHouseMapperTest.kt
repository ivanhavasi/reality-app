package cz.havasi.reality.app.sreality

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.CurrencyType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.GetRealEstatesCommand
import cz.havasi.reality.app.model.type.ProviderType
import cz.havasi.reality.app.sreality.api.SrealityApi
import cz.havasi.reality.app.sreality.model.SrealitySearchResult
import kotlinx.coroutines.runBlocking
import org.jboss.resteasy.reactive.RestResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class SrealityHouseMapperTest {
    private val baseUrl = "https://www.sreality.cz"
    private val command = GetRealEstatesCommand(
        type = BuildingType.HOUSE,
        transaction = TransactionType.SALE,
        offset = 0,
        limit = 22,
    )
    private val searchResult: SrealitySearchResult = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .readValue(javaClass.getResource("/fixtures/sreality-house-search.json")!!)

    private fun estate(hashId: String) = searchResult.results.first { it.hashId == hashId }

    @Test
    fun `family house takes usable and plot area from name`() {
        val house = estate("451055692").toHouse(command, baseUrl)!!

        assertEquals(44_900_000.0, house.price)
        assertEquals(392.0, house.sizeInM2)
        assertEquals(1068.0, house.landSizeInM2)
        assertEquals(44_900_000.0 / 392, house.pricePerM2)
        assertEquals("FAMILY_HOUSE", house.subCategory)
        assertEquals(BuildingType.HOUSE, house.mainCategory)
        assertEquals(TransactionType.SALE, house.transactionType)
        assertEquals(CurrencyType.CZK, house.currency)
        assertEquals(ProviderType.SREALITY, house.provider)
        assertEquals("Dolní Měcholupy", house.locality.district)
        assertEquals("Na Návsi", house.locality.street)
        assertEquals("house-Praha-SALE-392", house.fingerprint)
    }

    @Test
    fun `same house listed as a villa shares the fingerprint but keeps its subtype`() {
        val villa = estate("99782732").toHouse(command, baseUrl)!!

        assertEquals("VILLA", villa.subCategory)
        assertEquals("house-Praha-SALE-392", villa.fingerprint)
    }

    @Test
    fun `price on request house has zero price and zero price per m2`() {
        val house = estate("933176140").toHouse(command, baseUrl)!!

        assertEquals(0.0, house.price)
        assertEquals(0.0, house.pricePerM2)
        assertEquals(139.0, house.sizeInM2)
        assertEquals(210.0, house.landSizeInM2)
    }

    @Test
    fun `cottage maps to COTTAGE`() {
        val cottage = estate("2137755724").toHouse(command, baseUrl)!!

        assertEquals("COTTAGE", cottage.subCategory)
        assertEquals(10.0, cottage.sizeInM2)
        assertEquals(169.0, cottage.landSizeInM2)
    }

    @Test
    fun `placeholder plot is dropped and unknown subtype stays empty`() {
        val house = estate("5555").toHouse(command.copy(transaction = TransactionType.RENT), baseUrl)!!

        assertEquals(65.0, house.sizeInM2)
        assertNull(house.landSizeInM2)
        assertNull(house.subCategory)
        assertNull(house.locality.district)
        assertEquals(TransactionType.RENT, house.transactionType)
        assertEquals("house-Praha-RENT-65", house.fingerprint)
    }

    @Test
    fun `house url points to dum detail with the type slug`() {
        assertEquals(
            "https://www.sreality.cz/detail/prodej/dum/rodinny/praha-dolni-mecholupy-na-navsi/451055692",
            estate("451055692").toHouse(command, baseUrl)!!.url,
        )
        assertEquals(
            "https://www.sreality.cz/detail/prodej/dum/vila/praha-dolni-mecholupy-na-navsi/99782732",
            estate("99782732").toHouse(command, baseUrl)!!.url,
        )
        assertEquals(
            "https://www.sreality.cz/detail/prodej/dum/chata/praha-stresovice-delostrelecka/2137755724",
            estate("2137755724").toHouse(command, baseUrl)!!.url,
        )
        assertEquals(
            "https://www.sreality.cz/detail/pronajem/dum/rodinny/praha--v-sedlci/5555",
            estate("5555").toHouse(command.copy(transaction = TransactionType.RENT), baseUrl)!!.url,
        )
    }

    @Test
    fun `house without area in name is skipped`() {
        assertNull(estate("6666").toHouse(command, baseUrl))
    }

    @Test
    fun `house fetch asks for category 2 without sub category`() = runBlocking {
        var sentCategoryMain: Int? = null
        var sentCategorySub: String? = "not called"
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
                sentCategoryMain = categoryMain
                sentCategorySub = categorySub
                return RestResponse.ok(searchResult)
            }
        }

        val result = SrealityRealEstatesProvider(api, baseUrl).getRealEstates(command)

        assertEquals(listOf("451055692", "99782732", "933176140", "2137755724", "5555"), result.map { it.id })
        assertEquals(2, sentCategoryMain)
        assertNull(sentCategorySub)
    }
}
