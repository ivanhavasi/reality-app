package cz.havasi.reality.app.sreality

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.GetRealEstatesCommand
import cz.havasi.reality.app.model.type.ProviderType
import cz.havasi.reality.app.service.util.constructFingerprint
import cz.havasi.reality.app.service.util.plotSizeOrNull
import cz.havasi.reality.app.sreality.model.SrealityApartment
import cz.havasi.reality.app.sreality.model.SrealityHouseSubCategory
import io.quarkus.logging.Log

// "Prodej rodinného domu 100 m², pozemek 146 m²": usable area first, then the plot
private val USABLE_AREA_REGEX = Regex("""(\d+)[\s ]*m²""")
private val PLOT_AREA_REGEX = Regex("""pozemek[\s ]+(\d+)[\s ]*m²""")

internal fun SrealityApartment.toHouse(command: GetRealEstatesCommand, baseUrl: String): Apartment? {
    val size = USABLE_AREA_REGEX.find(name)?.groupValues?.get(1)?.toDouble()
        ?: return null.also { Log.warn("Sreality house $hashId has no area in '$name', skipping") }
    val houseLocality = locality.toCityPartLocality()
    val houseSubCategory = SrealityHouseSubCategory.fromCode(subCategory?.value)
    return Apartment(
        id = hashId,
        fingerprint = constructFingerprint(BuildingType.HOUSE, houseLocality, "", command.transaction, size),
        name = name,
        url = prepareHouseUrl(baseUrl, command.transaction, houseSubCategory),
        price = price,
        pricePerM2 = price / size,
        sizeInM2 = size,
        landSizeInM2 = PLOT_AREA_REGEX.find(name)?.groupValues?.get(1)?.toDouble()?.plotSizeOrNull(),
        currency = currency.name.toCurrencyType(),
        locality = houseLocality,
        mainCategory = BuildingType.HOUSE,
        subCategory = houseSubCategory?.canonical?.name,
        transactionType = command.transaction,
        images = images.map { "https:$it?fl=res,800,600,3|shr,,20|webp,60" },
        provider = ProviderType.SREALITY,
    )
}

// a wrong type slug still works, Sreality answers it with a 301 to the right one
private fun SrealityApartment.prepareHouseUrl(
    baseUrl: String,
    transaction: TransactionType,
    houseSubCategory: SrealityHouseSubCategory?,
): String {
    val transactionSlug = if (transaction == TransactionType.SALE) "prodej" else "pronajem"
    val subSlug = (houseSubCategory ?: SrealityHouseSubCategory.FAMILY_HOUSE).detailSlug
    return "$baseUrl/detail/$transactionSlug/dum/$subSlug/${locality.citySeoName ?: ""}-${locality.citypartSeoName ?: ""}-${locality.streetSeoName ?: ""}/$hashId"
}
