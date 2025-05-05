package cz.havasi.reality.app.idnes

import cz.havasi.reality.app.idnes.api.IdnesApi
import cz.havasi.reality.app.model.*
import cz.havasi.reality.app.model.command.GetRealEstatesCommand
import cz.havasi.reality.app.model.type.ProviderType
import cz.havasi.reality.app.service.provider.RealEstatesProvider
import cz.havasi.reality.app.service.util.constructFingerprint
import cz.havasi.reality.app.service.util.firstCapitalOthersLowerCase
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.resteasy.reactive.RestResponse
import org.jsoup.Jsoup
import kotlin.math.roundToInt

@ApplicationScoped
internal class IdnesRealEstateProvider(
    @RestClient private val idnesApi: IdnesApi,
) : RealEstatesProvider {
    override suspend fun getRealEstates(getRealEstatesCommand: GetRealEstatesCommand): List<Apartment> =
        with(getRealEstatesCommand) {
            try {
                callClient()
            } catch (e: Exception) {
                Log.error("Error while fetching iDnes data, page ${calculatePage()}", e)
                emptyList()
            }
        }

    private suspend fun GetRealEstatesCommand.callClient(): List<Apartment> =
    // idnes does not support pagination
    // you can only do other pages
        // always returns 21 results
        idnesApi.searchEstatesForPageZero(
            transactionType = getTransactionType(),
            buildingType = getBuildingType(),
            location = getLocation(),
            page = calculatePage(),
        )
            .handleResponse()
            .parseDataFromResponse(this)

    private fun RestResponse<String>.handleResponse(): String {
        if (status != 200) {
            Log.error("Error while fetching iDnes data, status code: $status, $entity")
            throw RuntimeException("Error while fetching iDnes data, status code: $status, $entity")
        }
        return entity
    }

    // ugly html parsing
    private fun String.parseDataFromResponse(getRealEstatesCommand: GetRealEstatesCommand): List<Apartment> =
        Jsoup.parse(this).select(".c-products__inner").map {
            val linkElement = it.selectFirst("a.c-products__link")
            val titleElement = it.selectFirst(".c-products__title")

            val splitUrl = linkElement?.attr("href")?.split("/") ?: emptyList()
            val id = splitUrl.getOrNull(splitUrl.lastIndex - 1) ?: logMissingData("id")
            val name = titleElement?.text()?.firstCapitalOthersLowerCase() ?: logMissingData("name")
            val url = linkElement?.attr("href") ?: logMissingData("url")
            val price = it.selectFirst(".c-products__price strong")?.text()?.replace("Kč", "")?.replace(" ", "")?.trim()
                ?.toDoubleOrNull() ?: 0.0
            val localityText = it.selectFirst(".c-products__info")?.text() ?: logMissingData("locality")
            val localityParts = localityText.split(",").map { it.trim() }
            val city = "Praha" // todo currently supports prague only
            val district = localityParts.getOrNull(1)?.split("-")?.getOrNull(1)?.trim() ?: logMissingData("district")
            val street = localityParts.getOrNull(0) ?: logMissingData("street")

            val size = name.split(" ").getOrNull(3)?.toDoubleOrNull() ?: 1.0
            val pricePerM2 = price / size
            val imageUrl = it.selectFirst(".c-products__img img")?.attr("data-src") ?: logMissingData("image")
            val rawSubCategory = name.split(" ").getOrNull(2)
            val subCategory = if (rawSubCategory == "atypického") "atypicky" else rawSubCategory

            val locality = Locality(
                city = city,
                district = district,
                street = street,
                streetNumber = null,
                latitude = null,
                longitude = null,
            )

            Apartment(
                id = id,
                fingerprint = constructFingerprint(getRealEstatesCommand.type, locality, subCategory ?: ""),
                name = name,
                url = url,
                price = price,
                pricePerM2 = pricePerM2,
                sizeInM2 = size.roundToInt().toDouble(), // rounding
                currency = CurrencyType.CZK,
                locality = locality,
                mainCategory = getRealEstatesCommand.type,
                subCategory = subCategory,
                transactionType = getRealEstatesCommand.transaction,
                images = listOf(imageUrl),
                provider = ProviderType.IDNES,
            )
        }

    private fun logMissingData(data: String): String {
        Log.error("Missing $data while scraping iDnes reality")

        return "Unknown $data"
    }

    private fun GetRealEstatesCommand.getTransactionType(): String =
        when (transaction) {
            TransactionType.SALE -> "prodej"
            TransactionType.RENT -> "pronajem"
        }

    private fun GetRealEstatesCommand.getBuildingType(): String =
        when (type) {
            BuildingType.HOUSE -> "domy"
            BuildingType.LAND -> "pozemky" // todo verify
            BuildingType.APARTMENT -> "byty"
        }

    private fun GetRealEstatesCommand.getLocation(): String = "praha"

    private fun GetRealEstatesCommand.calculatePage(): Int? =
        let { offset / limit }
            .takeIf { it > 0 }
}
