package cz.havasi.service.provider

import SrealityApartment
import SrealityLocality
import SrealityProperty
import cz.havasi.model.*
import cz.havasi.model.command.GetRealEstatesCommand
import cz.havasi.model.enum.ProviderType
import cz.havasi.rest.client.SrealityClient
import cz.havasi.service.util.constructFingerprint
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RestClient
import kotlin.math.roundToInt

@ApplicationScoped
public class SrealityProvider internal constructor(
    @RestClient private val srealityClient: SrealityClient,
    @ConfigProperty(name = "quarkus.rest-client.sreality-api.url") private val baseUrl: String,
) : RealEstatesProvider {
    override suspend fun getRealEstates(getRealEstatesCommand: GetRealEstatesCommand): List<Apartment> = with(getRealEstatesCommand) {
        try {
            callClient()
        } catch (e: Exception) {
            Log.error("Error while fetching sreality data, limit $limit, offset $offset", e)
            emptyList()
        }
    }

    private suspend fun GetRealEstatesCommand.callClient(): List<Apartment> =
        srealityClient.searchEstates(
            categoryType = 1,
            categoryMain = 1,
            localityCountryId = 112,
            localityRegionId = 10,
            limit = limit,
            offset = offset,
            lang = "cs",
            sort = "-date",
            topTimestampTo = System.currentTimeMillis(),
        )
            .results
            .also { Log.info("Sreality estates found size: ${it.size}") }
            .map { it.toApartment() }

    private fun SrealityApartment.toApartment() =
        Apartment(
            id = hashId,
            fingerprint = constructFingerprint(BuildingType.APARTMENT, locality.toLocality(), subCategory?.name ?: ""),
            name = name,
            url = prepareUrl(),
            price = price,
            pricePerM2 = pricePerM2 ?: error("Sreality apartment $hashId has no price per m2"),
            sizeInM2 = calculateSizeInM2(), // rounding
            currency = currency.name.toCurrencyType(),
            locality = locality.toLocality(),
            mainCategory = mainCategory?.name?.toMainCategory()
                ?: error("Sreality apartment $hashId has no main category"),
            subCategory = subCategory?.name,
            transactionType = transactionType?.toTransactionType()
                ?: error("Sreality apartment $hashId has no transaction type"),
            images = images.map { "https:$it?fl=res,800,600,3|shr,,20|webp,60" },
            provider = ProviderType.SREALITY,
        )

    private fun SrealityApartment.calculateSizeInM2() =
        try {
            val pricePerM2Updated = pricePerM2 ?: 1.0
            (price / pricePerM2Updated).roundToInt().toDouble()
        } catch (e: Exception) {
            Log.error("Error while calculating size in m2 for apartment $hashId", e)
            0.0
        }

    private fun SrealityApartment.prepareUrl() =
        "$baseUrl/detail/prodej/byt/${subCategory?.name}/${locality.citySeoName ?: ""}-${locality.citypartSeoName ?: ""}-${locality.streetSeoName ?: ""}/$hashId"

    private fun SrealityLocality.toLocality() =
        Locality(
            city = city,
            district = district,
            street = street,
            streetNumber = streetNumber,
            latitude = latitude,
            longitude = longitude,
        )

    private fun String.toCurrencyType() = when (this) { // todo, maybe it can be done via value and not string name
        "Kč" -> CurrencyType.CZK
        "€" -> CurrencyType.EUR
        "$" -> CurrencyType.USD
        else -> error("Unknown currency type: $this")
    }

    private fun String.toMainCategory() = when (this) { // todo, maybe it can be done via value and not string name
        "Byty" -> BuildingType.APARTMENT
        "Domy" -> BuildingType.HOUSE
        "Pozemky" -> BuildingType.LAND
        else -> error("Unknown main category: $this")
    }

    private fun SrealityProperty.toTransactionType() =
        when (name) { // todo, maybe it can be done via value and not string name
            "Prodej" -> TransactionType.SALE
            "Pronájem" -> TransactionType.RENT
            else -> error("Unknown transaction type: $name")
        }

}
