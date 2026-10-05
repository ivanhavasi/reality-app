package cz.havasi.reality.app.idnes

import cz.havasi.reality.app.model.Apartment
import cz.havasi.reality.app.model.BuildingType
import cz.havasi.reality.app.model.CurrencyType
import cz.havasi.reality.app.model.Locality
import cz.havasi.reality.app.model.TransactionType
import cz.havasi.reality.app.model.command.GetRealEstatesCommand
import cz.havasi.reality.app.model.type.LandSubCategory
import cz.havasi.reality.app.model.type.ProviderType
import cz.havasi.reality.app.service.util.constructFingerprint
import cz.havasi.reality.app.service.util.firstCapitalOthersLowerCase
import cz.havasi.reality.app.service.util.looksLikePricePerM2
import io.quarkus.logging.Log
import org.jsoup.nodes.Element

internal val LandSubCategory.idnesSlug: String?
    get() = when (this) {
        LandSubCategory.BUILDING_PLOT -> "stavebni-pozemek"
        LandSubCategory.COMMERCIAL -> "pro-komercni-vyuziti"
        LandSubCategory.FIELD -> "zemedelsky"
        LandSubCategory.FOREST -> "les"
        LandSubCategory.GARDEN -> "zahrady"
        LandSubCategory.MEADOW -> "louka"
        LandSubCategory.POND -> "vodni-plocha"
        LandSubCategory.OTHER -> "jine"
        LandSubCategory.ORCHARD_VINEYARD -> null
    }

// (?:^|\s) stops "2+1 65 m²" being read as 165
private val SIZE_REGEX = Regex("""(?:^|\s)(\d{1,3}(?:[\s ]\d{3})*(?:,\d+)?)[\s\u00A0]*m²""")

private val LAND_TITLE_NOUNS = mapOf(
    "stavebního pozemku" to LandSubCategory.BUILDING_PLOT,
    "komerčního pozemku" to LandSubCategory.COMMERCIAL,
    "zahrady" to LandSubCategory.GARDEN,
    "pole" to LandSubCategory.FIELD,
    "lesa" to LandSubCategory.FOREST,
    "louky" to LandSubCategory.MEADOW,
    "vodní plochy" to LandSubCategory.POND,
    "pozemku" to LandSubCategory.OTHER,
)

internal fun String.parseSize(): Double? =
    SIZE_REGEX.find(this)?.groupValues?.get(1)
        ?.filter { it.isDigit() || it == ',' }
        ?.replace(',', '.')
        ?.toDoubleOrNull()

// "Prodej stavebního pozemku 1 418 m²" -> "stavebního pozemku"
internal fun String.parseLandSubCategory(): LandSubCategory? =
    lowercase().split(" ").drop(1)
        .takeWhile { word -> word.firstOrNull()?.isLetter() == true }
        .joinToString(" ")
        .let { LAND_TITLE_NOUNS[it] }

internal fun Element.toLand(command: GetRealEstatesCommand): Apartment? {
    val linkElement = selectFirst("a.c-products__link") ?: return null
    val url = linkElement.attr("href")
    val id = url.split("/").let { it.getOrNull(it.lastIndex - 1) }?.ifBlank { null }
        ?: return null.also { Log.warn("iDnes land card has no id in '$url', skipping") }
    val name = selectFirst(".c-products__title")?.text()?.firstCapitalOthersLowerCase()
        ?: return null.also { Log.warn("iDnes land $id has no title, skipping") }
    val size = name.parseSize() ?: return null.also { Log.warn("iDnes land $id has no area in '$name', skipping") }
    val subCategory = name.parseLandSubCategory() ?: command.landSubCategory
    val price = getPrice(command.transaction)
    // Bezrealitky syndicates its adverts here; this also catches tiny-share listings
    if (subCategory == LandSubCategory.BUILDING_PLOT && looksLikePricePerM2(price, size)) {
        Log.warn("iDnes land $id: $price Kč for $size m² looks like a per-m² price, skipping")
        return null
    }
    val locality = selectFirst(".c-products__info")?.text()?.toLandLocality()
        ?: return null.also { Log.warn("iDnes land $id has no locality, skipping") }
    return Apartment(
        id = id,
        fingerprint = constructFingerprint(BuildingType.LAND, locality, subCategory?.name ?: "", command.transaction, size),
        name = name,
        url = url,
        price = price,
        pricePerM2 = price / size,
        sizeInM2 = size,
        currency = CurrencyType.CZK,
        locality = locality,
        mainCategory = BuildingType.LAND,
        subCategory = subCategory?.name,
        transactionType = command.transaction,
        images = listOfNotNull(selectFirst(".c-products__img img")?.attr("data-src")?.ifBlank { null }),
        provider = ProviderType.IDNES,
    )
}

internal fun Element.getPrice(transactionType: TransactionType): Double {
    val replaceString = when (transactionType) {
        TransactionType.RENT -> "Kč/měsíc"
        TransactionType.SALE -> "Kč"
    }

    return selectFirst(".c-products__price strong")
        ?.text()
        ?.replace(replaceString, "")
        ?.replace(" ", "")
        ?.replace(" ", "")
        ?.replace(",", ".") // "17 999 999,44 Kč"
        ?.trim()
        ?.toDoubleOrNull()
        ?: 0.0
}

// Prague: "[Ulice, ]Praha[ N][ - Část]"
private fun String.toLandLocality(): Locality {
    val parts = split(",").map { it.trim() }.filter { it.isNotEmpty() }
    return Locality(
        city = "Praha",
        district = parts.lastOrNull()?.substringAfter(" - ", "")?.ifBlank { null },
        street = parts.takeIf { it.size > 1 }?.first(),
        streetNumber = null,
        latitude = null,
        longitude = null,
    )
}
