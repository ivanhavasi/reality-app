package cz.havasi.sreality

import SrealityApartment
import SrealityLocality
import SrealityProperty
import cz.havasi.model.Apartment
import cz.havasi.model.BuildingType
import cz.havasi.model.CurrencyType
import cz.havasi.model.GetEstatesCommand
import cz.havasi.model.Locality
import cz.havasi.model.TransactionType
import cz.havasi.service.EstatesProvider
import cz.havasi.sreality.client.SrealityClient
import io.quarkus.logging.Log
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient

@ApplicationScoped
public class SrealityService internal constructor( // todo, add it into new module
    @RestClient private val srealityClient: SrealityClient,
) : EstatesProvider {

    override suspend fun getEstates(getEstatesCommand: GetEstatesCommand): List<Apartment> {
        Log.info("Searching sreality estates")
        return srealityClient.searchEstates(
            categoryType = 1,
            categoryMain = 1,
            localityCountryId = 112,
            localityRegionId = 10,
            limit = 22,
            offset = 0,
            lang = "cs",
            sort = "-date",
            topTimestampTo = System.currentTimeMillis(),
        )
            .results
            .also { Log.info("Sreality estates found size: ${it.size}") }
            .map { it.toApartment() }
    }

    private fun SrealityApartment.toApartment() =
        Apartment(
            id = hashId,
            fingerprint = calculateFingerprint(),
            name = name,
            price = price,
            pricePerM2 = pricePerM2 ?: error("Sreality apartment $hashId has no price per m2"),
            sizeInM2 = price / pricePerM2,
            currency = currency.name.toCurrencyType(),
            locality = locality.toLocality(),
            mainCategory = mainCategory?.name?.toMainCategory()
                ?: error("Sreality apartment $hashId has no main category"),
            subCategory = subCategory?.name,
            transactionType = transactionType?.toTransactionType()
                ?: error("Sreality apartment $hashId has no transaction type"),
            images = images,
        )

    private fun SrealityApartment.calculateFingerprint() =
        "${mainCategory?.name}-${locality.city}-${locality.street}-${images.size}" // todo, add hash function

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
        "Kč" -> CurrencyType.CZ
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
