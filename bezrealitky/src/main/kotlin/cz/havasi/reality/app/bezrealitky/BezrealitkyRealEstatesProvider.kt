package cz.havasi.reality.app.bezrealitky

import cz.havasi.reality.app.bezrealitky.api.BezrealitkyApi
import cz.havasi.reality.app.model.*
import cz.havasi.reality.app.model.command.GetRealEstatesCommand
import cz.havasi.reality.app.model.type.ProviderType
import cz.havasi.reality.app.service.provider.RealEstatesProvider
import cz.havasi.reality.app.service.util.constructFingerprint
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.WebApplicationException
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jsoup.Jsoup
import kotlin.math.roundToInt

@ApplicationScoped
internal class BezrealitkyRealEstatesProvider(
    @RestClient private val bezrealitkyApi: BezrealitkyApi,
) : RealEstatesProvider {
    override suspend fun getRealEstates(getRealEstatesCommand: GetRealEstatesCommand): List<Apartment> =
        with(getRealEstatesCommand) {
            try {
                if (type == BuildingType.LAND && prepareLandType() == null) {
                    Log.debug("Bezrealitky can't classify unfiltered land results, skipping $landSubCategory")
                    return emptyList()
                }
                callClient()
            } catch (e: WebApplicationException) {
                // past the last page Bezrealitky redirects to a generic feed, which must never be followed
                if (e.response.status in 300..399) {
                    Log.debug("Bezrealitky page ${calculatePage()} redirected, end of results")
                } else {
                    Log.error("Error while fetching Bezrealitky data, page ${calculatePage()}", e)
                }
                emptyList()
            } catch (e: Exception) {
                Log.error("Error while fetching Bezrealitky data, page ${calculatePage()}", e)
                emptyList()
            }
        }

    private suspend fun GetRealEstatesCommand.callClient(): List<Apartment> =
        // bezrealitky doesn't support pagination, same as iDnes
        bezrealitkyApi.searchEstates(
            offerType = prepareOfferType(),
            estateType = prepareEstateType(),
            landType = prepareLandType(),
            osmValue = "Hlavn%C3%AD+m%C4%9Bsto+Praha%2C+Praha%2C+%C4%8Cesko", // locality - Prague
            regionOsmIds = "R435514", // locality - Prague
            currency = "CZK",
            location = "exact",
            page = calculatePage(),
        )
            .parseDataFromResponse(this)

    // ugly html parsing
    private fun String.parseDataFromResponse(getRealEstatesCommand: GetRealEstatesCommand): List<Apartment> =
        Jsoup.parse(this).select("article.PropertyCard_propertyCard__moO_5").mapNotNull {
            if (getRealEstatesCommand.type == BuildingType.LAND) return@mapNotNull it.toLand(getRealEstatesCommand)
            val url = it.selectFirst(".PropertyCard_propertyCardHeadline___diKI")?.selectFirst("a")?.attr("href")
            val id = url?.substringAfter("/nemovitosti-byty-domy/") ?: logMissingData("id")
            val images = it.parseImages()
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
                    buildingType = getRealEstatesCommand.type,
                    locality = locality,
                    subCategory = subCategory ?: "",
                    transactionType = getRealEstatesCommand.transaction,
                ),
                name = name,
                pricePerM2 = price / size,
                sizeInM2 = size.roundToInt().toDouble(), // rounding
                currency = CurrencyType.CZK,
                mainCategory = getRealEstatesCommand.type,
                subCategory = subCategory,
                transactionType = getRealEstatesCommand.transaction,
                provider = ProviderType.BEZREALITKY,
            )
        }

    private fun logMissingData(data: String): String =
        data
            .also { Log.error("Missing $data while scraping Bezrealitky") }
            .let { "Unknown $it" }

    private fun GetRealEstatesCommand.prepareOfferType() = when (transaction) {
        TransactionType.SALE -> "PRODEJ"
        TransactionType.RENT -> "PRONAJEM"
    }

    private fun GetRealEstatesCommand.prepareEstateType() = when (type) {
        BuildingType.APARTMENT -> "BYT"
        BuildingType.HOUSE -> "DUM"
        BuildingType.LAND -> "POZEMEK"
    }

    private fun GetRealEstatesCommand.calculatePage() = (offset / limit) + 1
}
