package cz.havasi.service.provider

import cz.havasi.model.Apartment
import cz.havasi.model.BuildingType
import cz.havasi.model.CurrencyType
import cz.havasi.model.Locality
import cz.havasi.model.TransactionType
import cz.havasi.model.command.GetEstatesCommand
import cz.havasi.rest.client.BezrealitkyClient
import cz.havasi.service.constructFingerprint
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@ApplicationScoped
internal class BezrealitkyProvider(
    @RestClient private val bezrealitkyClient: BezrealitkyClient,
    @ConfigProperty(name = "quarkus.rest-client.bezrealitky-api.url") private val baseUrl: String,
) : EstatesProvider {
    override suspend fun getEstates(getEstatesCommand: GetEstatesCommand): List<Apartment> = with(getEstatesCommand) {
        // bezrealitky doesn't support pagination, same as iDnes
        bezrealitkyClient.searchEstates(
            offerType = prepareOfferType(),
            estateType = prepareEstateType(),
            osmValue = "Hlavn%C3%AD+m%C4%9Bsto+Praha%2C+Praha%2C+%C4%8Cesko", // locality - Prague
            regionOsmIds = "R435514", // locality - Prague
            currency = "CZK",
            location = "exact",
            page = calculatePage(),
        )
            .parseDataFromResponse(getEstatesCommand)
    }

    // ugly html parsing
    private fun String.parseDataFromResponse(getEstatesCommand: GetEstatesCommand): List<Apartment> =
        Jsoup.parse(this).select("article.PropertyCard_propertyCard__moO_5").map {
            val url = it.selectFirst(".PropertyCard_propertyCardHeadline___diKI")?.selectFirst("a")?.attr("href")
            val id = url?.substringAfter("/nemovitosti-byty-domy/") ?: logMissingData("id")
            val images = it.select("img").mapNotNull { it.attr("src").substringAfter("/_next/image?url=") }.map {
                URLDecoder.decode(it, StandardCharsets.UTF_8)
            }.map { it.substringBefore("&w=") }
            val name = it.selectFirst(".PropertyCard_propertyCardHeadline___diKI")?.text() ?: logMissingData("name")
            val price = it.selectFirst(".PropertyPrice_propertyPriceAmount__WdEE1")?.text()?.substringBefore("Kč")
                ?.filter { it != ' ' }?.toDoubleOrNull() ?: 0.0
            val localityText =
                it.selectFirst(".PropertyCard_propertyCardAddress__hNqyR")?.text() ?: logMissingData("locality")
            val localityParts = localityText.split(",").map { it.trim() }
            val city = "Praha"
            val street = localityParts.getOrNull(0) ?: logMissingData("street")
            val district = localityParts.getOrNull(1)?.split("-")?.getOrNull(1)?.trim() ?: logMissingData("district")
            val description = it.selectFirst(".propertyCardDescription")?.text()
            val sizeAndSubType = it.getElementsByClass("FeaturesList_featuresList__75Wet")
            val size =
                sizeAndSubType.select("li").getOrNull(1)?.text()?.split("m")?.getOrNull(0)?.toDoubleOrNull() ?: 1.0
            val subType = sizeAndSubType.select("li").getOrNull(0)?.text()?.trim()
            val subCategory = if (subType == "Ostatní") "atypicky" else if (subType == "Garsoniéra") "1+kk" else subType

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
                url = url ?: logMissingData("url"),
                images = images,
                price = price,
                locality = locality,
                description = description,
                fingerprint = constructFingerprint(
                    buildingType = getEstatesCommand.type,
                    locality = locality,
                    subCategory = subCategory ?: "",
                ),
                name = name,
                pricePerM2 = price / size,
                sizeInM2 = size,
                currency = CurrencyType.CZK,
                mainCategory = getEstatesCommand.type,
                subCategory = subCategory,
                transactionType = getEstatesCommand.transaction,
            )
        }

    private fun logMissingData(data: String): String {
        Log.error("Missing $data while scraping Bezrealitky")

        return "Unknown $data"
    }

    private fun GetEstatesCommand.prepareOfferType() = when (transaction) {
        TransactionType.SALE -> "PRODEJ"
        TransactionType.RENT -> "PRONAJEM"
    }

    private fun GetEstatesCommand.prepareEstateType() = when (type) {
        BuildingType.APARTMENT -> "BYT"
        BuildingType.HOUSE -> "DUM"
        BuildingType.LAND -> "POZEMEK"
    }

    private fun GetEstatesCommand.calculatePage() = (offset / limit) + 1
}
