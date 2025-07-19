package cz.havasi.reality.app.sreality

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.CurrencyType
import cz.havasi.reality.app.model.Locality
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.GetRealEstatesCommand
import cz.havasi.reality.app.model.type.ProviderType
import cz.havasi.reality.app.service.provider.RealEstatesProvider
import cz.havasi.reality.app.service.util.constructFingerprint
import cz.havasi.reality.app.sreality.api.SrealityApi
import cz.havasi.reality.app.sreality.model.SrealityApartment
import cz.havasi.reality.app.sreality.model.SrealityLocality
import cz.havasi.reality.app.sreality.model.SrealityProperty
import cz.havasi.reality.app.sreality.model.SrealitySearchResult
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.resteasy.reactive.RestResponse
import kotlin.math.roundToInt

@ApplicationScoped
internal class SrealityRealEstatesProvider(
    @RestClient private val srealityApi: SrealityApi,
    @ConfigProperty(name = "quarkus.rest-client.sreality-api.url") private val baseUrl: String,
) : RealEstatesProvider {
    override suspend fun getRealEstates(getRealEstatesCommand: GetRealEstatesCommand): List<Apartment> =
        with(getRealEstatesCommand) {
            try {
                callClient()
            } catch (e: Exception) {
                Log.error("Error while fetching sreality data, limit $limit, offset $offset", e)
                emptyList()
            }
        }

    private suspend fun GetRealEstatesCommand.callClient(): List<Apartment> =
        srealityApi.searchEstates(
            categoryType = resolveCategoryType(),
            categoryMain = resolveCategoryMain(),
            localityCountryId = 112,
            localityRegionId = 10,
            limit = limit,
            offset = offset,
            lang = "cs",
            sort = "-date",
            topTimestampTo = System.currentTimeMillis(),
        )
            .handleResult()
            .results
            .also { Log.info("Sreality estates found size: ${it.size}") }
            .map { it.toApartment(this) }

    private fun GetRealEstatesCommand.resolveCategoryType(): Int =
        when (transaction) {
            TransactionType.SALE -> 1
            TransactionType.RENT -> 2
        }

    private fun GetRealEstatesCommand.resolveCategoryMain(): Int =
        when (type) {
            BuildingType.APARTMENT -> 1
            BuildingType.HOUSE -> 2
            BuildingType.LAND -> 3
        }

    private fun RestResponse<SrealitySearchResult>.handleResult(): SrealitySearchResult {
        check(status == 200) { "Sreality API returned status $status" }
        return entity ?: error("Sreality API returned empty body")
    }

    private fun SrealityApartment.toApartment(command: GetRealEstatesCommand) =
        Apartment(
            id = hashId,
            fingerprint = constructFingerprint(
                BuildingType.APARTMENT,
                locality.toLocality(),
                subCategory?.name ?: "",
                command.transaction,
            ),
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

    private fun String.toCurrencyType() = when (this) {
        "Kč" -> CurrencyType.CZK
        "€" -> CurrencyType.EUR
        "$" -> CurrencyType.USD
        else -> error("Unknown currency type: $this")
    }

    private fun String.toMainCategory() = when (this) {
        "Byty" -> BuildingType.APARTMENT
        "Domy" -> BuildingType.HOUSE
        "Pozemky" -> BuildingType.LAND
        else -> error("Unknown main category: $this")
    }

    private fun SrealityProperty.toTransactionType() =
        when (name) {
            "Prodej" -> TransactionType.SALE
            "Pronájem" -> TransactionType.RENT
            else -> error("Unknown transaction type: $name")
        }
}
