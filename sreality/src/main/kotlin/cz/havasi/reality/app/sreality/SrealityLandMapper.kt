package cz.havasi.reality.app.sreality

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.CurrencyType
import cz.havasi.reality.app.model.Locality
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.GetRealEstatesCommand
import cz.havasi.reality.app.model.type.ProviderType
import cz.havasi.reality.app.service.util.constructFingerprint
import cz.havasi.reality.app.sreality.model.SrealityApartment
import cz.havasi.reality.app.sreality.model.SrealityLandSubCategory
import cz.havasi.reality.app.sreality.model.SrealityLocality
import io.quarkus.logging.Log

// advert_name separates the number and unit with an NBSP, which \s doesn't match
private val AREA_REGEX = Regex("""(\d+)[\s\u00A0]*m²""")
private const val PRICE_UNIT_PER_M2 = "3"
private const val MAX_PLAUSIBLE_PRICE_PER_M2 = 60_000.0

internal fun GetRealEstatesCommand.resolveCategorySub(): String? =
    if (type == BuildingType.LAND) landSubCategory?.let { SrealityLandSubCategory.from(it).code.toString() } else null

internal fun SrealityApartment.toLand(command: GetRealEstatesCommand, baseUrl: String): Apartment? {
    val size = AREA_REGEX.findAll(name).lastOrNull()?.groupValues?.get(1)?.toDouble()
        ?: return null.also { Log.warn("Sreality land $hashId has no area in '$name', skipping") }
    val price = resolveLandPrice()
    val landLocality = locality.toLandLocality()
    val landSubCategory = SrealityLandSubCategory.fromCode(subCategory?.value)
    val subCategory = landSubCategory?.canonical ?: command.landSubCategory
    return Apartment(
        id = hashId,
        fingerprint = constructFingerprint(
            BuildingType.LAND,
            landLocality,
            subCategory?.name ?: "",
            command.transaction,
            size,
        ),
        name = name,
        url = prepareLandUrl(baseUrl, command.transaction, landSubCategory),
        price = price,
        pricePerM2 = price / size,
        sizeInM2 = size,
        currency = currency.name.toCurrencyType(),
        locality = landLocality,
        mainCategory = BuildingType.LAND,
        subCategory = subCategory?.name,
        transactionType = command.transaction,
        images = images.map { "https:$it?fl=res,800,600,3|shr,,20|webp,60" },
        provider = ProviderType.SREALITY,
    )
}

// "za m²" listings carry the per-m² price in price_czk and the total in price_summary_czk.
// A "za m²" price above 60 000 is a total typed into the per-m² field.
private fun SrealityApartment.resolveLandPrice(): Double =
    if (priceUnit?.value == PRICE_UNIT_PER_M2 && price <= MAX_PLAUSIBLE_PRICE_PER_M2) priceSummary ?: price else price

private fun SrealityLocality.toLandLocality() =
    Locality(
        city = city,
        district = citypart?.ifBlank { null },
        street = street?.ifBlank { null },
        streetNumber = streetNumber?.ifBlank { null },
        latitude = latitude,
        longitude = longitude,
    )

private fun SrealityApartment.prepareLandUrl(
    baseUrl: String,
    transaction: TransactionType,
    landSubCategory: SrealityLandSubCategory?,
): String {
    val transactionSlug = if (transaction == TransactionType.SALE) "prodej" else "pronajem"
    val subSlug = (landSubCategory ?: SrealityLandSubCategory.BUILDING_PLOT).detailSlug
    return "$baseUrl/detail/$transactionSlug/pozemek/$subSlug/${locality.citySeoName ?: ""}-${locality.citypartSeoName ?: ""}-${locality.streetSeoName ?: ""}/$hashId"
}

internal fun String.toCurrencyType() = when (this) {
    "Kč" -> CurrencyType.CZK
    "€" -> CurrencyType.EUR
    "$" -> CurrencyType.USD
    else -> error("Unknown currency type: $this")
}
