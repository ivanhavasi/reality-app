package cz.havasi.service.provider

import cz.havasi.model.*
import cz.havasi.model.command.GetEstatesCommand
import cz.havasi.model.enum.ProviderType
import cz.havasi.rest.client.IdnesClient
import cz.havasi.service.util.constructFingerprint
import cz.havasi.service.util.firstCapitalOthersLowerCase
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jsoup.Jsoup

@ApplicationScoped
internal class IdnesProvider(
    @RestClient private val idnesClient: IdnesClient,
) : EstatesProvider {
    override suspend fun getEstates(getEstatesCommand: GetEstatesCommand): List<Apartment> = with(getEstatesCommand) {
        try {
            callClient()
        } catch (e: Exception) {
            Log.error("Error while fetching iDnes data, page ${calculatePage()}")
            Log.error(e.message)
            Log.error(e.stackTraceToString())
            emptyList()
        }
    }

    private suspend fun GetEstatesCommand.callClient(): List<Apartment> =
    // idnes does not support pagination
    // you can only do other pages
        // always returns 21 results
        idnesClient.searchEstatesForPageZero(
            transactionType = getTransactionType(),
            buildingType = getBuildingType(),
            location = getLocation(),
            page = calculatePage(),
        )
            .parseDataFromResponse(this)

    // ugly html parsing
    private fun String.parseDataFromResponse(getEstatesCommand: GetEstatesCommand): List<Apartment> =
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
                fingerprint = constructFingerprint(getEstatesCommand.type, locality, subCategory ?: ""),
                name = name,
                url = url,
                price = price,
                pricePerM2 = pricePerM2,
                sizeInM2 = size,
                currency = CurrencyType.CZK,
                locality = locality,
                mainCategory = getEstatesCommand.type,
                subCategory = subCategory,
                transactionType = getEstatesCommand.transaction,
                images = listOf(imageUrl),
                provider = ProviderType.IDNES,
            )
        }

    private fun logMissingData(data: String): String {
        Log.error("Missing $data while scraping iDnes reality")

        return "Unknown $data"
    }

    private fun GetEstatesCommand.getTransactionType(): String =
        when (transaction) {
            TransactionType.SALE -> "prodej"
            TransactionType.RENT -> "pronajem"
        }

    private fun GetEstatesCommand.getBuildingType(): String =
        when (type) {
            BuildingType.HOUSE -> "domy"
            BuildingType.LAND -> "pozemky" // todo verify
            BuildingType.APARTMENT -> "byty"
        }

    private fun GetEstatesCommand.getLocation(): String = "praha"

    private fun GetEstatesCommand.calculatePage(): Int? =
        let { offset / limit }
            .takeIf { it > 0 }
}
