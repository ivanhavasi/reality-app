package cz.havasi.reality.app.sreality

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.CurrencyType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.GetRealEstatesCommand
import cz.havasi.reality.app.sreality.api.SrealityApi
import cz.havasi.reality.app.sreality.model.SrealitySearchResult
import kotlinx.coroutines.runBlocking
import org.jboss.resteasy.reactive.RestResponse
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SrealityApartmentMapperTest {
    private val baseUrl = "https://www.sreality.cz"
    private val command = GetRealEstatesCommand(
        type = BuildingType.APARTMENT,
        transaction = TransactionType.RENT,
        offset = 0,
        limit = 22,
    )
    private val searchResult: SrealitySearchResult = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .readValue(javaClass.getResource("/fixtures/sreality-apartment-search.json")!!)

    private val api = object : SrealityApi {
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
        ): RestResponse<SrealitySearchResult> = RestResponse.ok(searchResult)
    }

    @Test
    fun `listing posted in EUR keeps its CZK converted price`() = runBlocking {
        val apartment = SrealityRealEstatesProvider(api, baseUrl).getRealEstates(command).first { it.id == "3134165068" }

        assertEquals("3134165068", apartment.id)
        assertEquals(140_901.03, apartment.price)
        assertEquals(844.0, apartment.pricePerM2)
        assertEquals(167.0, apartment.sizeInM2)
        assertEquals(CurrencyType.CZK, apartment.currency)
        assertEquals(TransactionType.RENT, apartment.transactionType)
    }

    @Test
    fun `hidden price listing takes its size from the name`() = runBlocking {
        val result = SrealityRealEstatesProvider(api, baseUrl).getRealEstates(command)
        val priceOnRequest = result.first { it.id == "494669900" }
        val oneCrown = result.first { it.id == "623054924" }

        assertEquals(0.0, priceOnRequest.price)
        assertEquals(135.0, priceOnRequest.sizeInM2)
        assertEquals(1.0, oneCrown.price)
        assertEquals(101.0, oneCrown.sizeInM2)
    }

    @Test
    fun `apartment without area in name is skipped and one broken estate does not drop the page`() = runBlocking {
        val result = SrealityRealEstatesProvider(api, baseUrl).getRealEstates(command)

        assertEquals(listOf("3134165068", "494669900", "623054924"), result.map { it.id })
    }
}
